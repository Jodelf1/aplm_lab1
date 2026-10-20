package com.jodelfi
import java.security.MessageDigest
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.UUID

private const val ULTIMA_DATA = 999

private fun sha256(texto: String): String {
    val bytes = MessageDigest.getInstance("SHA-256").digest(texto.toByteArray(Charsets.UTF_8))
    return bytes.joinToString("") { "%02x".format(it) }
}

private data class Movimento(
    val montante: Int,
    val data: Int,
    val tipo: Char,
    val descricao: String,
    val ordem: Long
)

private data class Sessao(
    val token: String,
    val dataHora: LocalDateTime
)

private class Utilizador(
    val email: String,
    password: String
) {
    private val passwordHash = sha256(password)
    val movimentos = mutableListOf<Movimento>()
    val sessoes = mutableListOf<Sessao>()
    var balancoMinimo: Int = 0
        private set
    private var proximaOrdem = 0L

    fun autenticar(password: String): Boolean = passwordHash == sha256(password)

    fun iniciarSessao(): Sessao {
        val sessao = Sessao(
            token = sha256(UUID.randomUUID().toString()),
            dataHora = LocalDateTime.now()
        )
        sessoes.add(sessao)
        return sessao
    }

    fun alterarBalancoMinimo(valor: Int) {
        balancoMinimo = valor
    }

    fun inserir(montante: Int, dataInicial: Int, periodicidade: Int, tipo: Char, descricao: String) {
        val ordemAtual = proximaOrdem++
        var data = dataInicial

        while (data <= ULTIMA_DATA) {
            movimentos.add(
                Movimento(
                    montante = montante,
                    data = data,
                    tipo = tipo,
                    descricao = descricao,
                    ordem = ordemAtual
                )
            )
            if (periodicidade == 0) break
            data += periodicidade
        }
    }

    fun movimentosNaData(data: Int): List<Movimento> =
        movimentos.filter { it.data == data }.sortedBy { it.ordem }

    fun remover(data: Int, indice: Int) {
        val lista = movimentosNaData(data)
        if (indice in lista.indices) movimentos.remove(lista[indice])
    }

    private fun calcularBalancos(): IntArray {
        val variacaoDiaria = IntArray(ULTIMA_DATA + 1)

        for (movimento in movimentos) {
            val fator = if (movimento.tipo == 'r') 1 else -1
            variacaoDiaria[movimento.data] += movimento.montante * fator
        }

        val balancos = IntArray(ULTIMA_DATA + 1)
        var atual = 0
        for (data in 0..ULTIMA_DATA) {
            atual += variacaoDiaria[data]
            balancos[data] = atual
        }
        return balancos
    }

    fun balanco(data: Int): Int = calcularBalancos()[data]

    fun verificarValidade(): Boolean =
        calcularBalancos().all { it >= balancoMinimo }

    fun dataComportavel(dataInicial: Int, despesa: Int): Int? {
        val balancos = calcularBalancos()

        for (candidata in dataInicial..ULTIMA_DATA) {
            var valida = true

            for (data in 0 until candidata) {
                if (balancos[data] < balancoMinimo) {
                    valida = false
                    break
                }
            }

            if (valida) {
                for (data in candidata..ULTIMA_DATA) {
                    if (balancos[data] - despesa < balancoMinimo) {
                        valida = false
                        break
                    }
                }
            }

            if (valida) return candidata
        }

        return null
    }
}

private class SistemaOrcamento {
    private val utilizadores = mutableMapOf<String, Utilizador>()
    private var utilizadorAtual: Utilizador? = null
    private var sessaoAtual: Sessao? = null

    fun executar() {
        while (true) {
            val linha = readlnOrNull() ?: break
            if (linha.isBlank()) continue

            val partes = linha.trim().split(Regex("\\s+"))
            if (partes[0] == "u") {
                iniciarSessao(partes)
                continue
            }

            if (partes[0] == "exit") {
                utilizadorAtual = null
                sessaoAtual = null
                continue
            }

            val utilizador = utilizadorAtual
            if (utilizador == null) {
                println("Inicie sessao com: u email password")
                continue
            }

            processarComando(utilizador, partes)
        }
    }

    private fun iniciarSessao(partes: List<String>) {
        val emailOriginal = partes[1]
        val email = emailOriginal.lowercase()
        val password = partes[2]

        val existente = utilizadores[email]
        val utilizador = if (existente == null) {
            Utilizador(email, password).also { utilizadores[email] = it }
        } else {
            if (!existente.autenticar(password)) {
                utilizadorAtual = null
                sessaoAtual = null
                println("Credenciais invalidas")
                return
            }
            existente
        }

        utilizadorAtual = utilizador
        sessaoAtual = utilizador.iniciarSessao()
        // O token e a data/hora ficam registados na sessão. Não são impressos,
        // porque o enunciado não define uma saída para o comando de autenticação.
    }

    private fun processarComando(utilizador: Utilizador, partes: List<String>) {
        when (partes[0]) {
            "i" -> utilizador.inserir(
                montante = partes[1].toInt(),
                dataInicial = partes[2].toInt(),
                periodicidade = partes[3].toInt(),
                tipo = partes[4][0],
                descricao = partes[5]
            )
            "l" -> {
                val data = partes[1].toInt()
                utilizador.movimentosNaData(data).forEachIndexed { indice, movimento ->
                    println("$indice ${movimento.montante} ${movimento.tipo} ${movimento.descricao}")
                }
            }
            "r" -> utilizador.remover(partes[1].toInt(), partes[2].toInt())
            "b" -> println(utilizador.balanco(partes[1].toInt()))
            "m" -> utilizador.alterarBalancoMinimo(partes[1].toInt())
            "v" -> println(if (utilizador.verificarValidade()) "Valido" else "Invalido")
            "w" -> {
                val data = partes[1].toInt()
                val despesa = partes[2].toInt()
                println(utilizador.dataComportavel(data, despesa) ?: "Incomportavel")
            }
            else -> println("Comando desconhecido")
        }
    }
}

fun main() {
    SistemaOrcamento().executar()
}