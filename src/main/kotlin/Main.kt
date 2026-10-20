package com.jodelfi

private const val ULTIMA_DATA = 999

data class Movimento(
    val montante: Int,
    val data: Int,
    val tipo: Char,
    val descricao: String
)

val movimentos = mutableListOf<Movimento>()
var balancoMinimo = 0

fun main() {
    while (true) {
        val linha = readlnOrNull() ?: break
        if (linha.trim() == "exit")
            break
        if (linha.isBlank()) {
            return
        }

        val partes = linha.trim().split(Regex("\\s+"))

        when (partes[0]) {
            "i" -> inserir(partes)

            "l" -> listar(partes[1].toInt())

            "r" -> remover(partes[1].toInt(),partes[2].toInt())

            "b" -> println(balanco(partes[1].toInt()))

            "m" -> balancoMinimo = partes[1].toInt()

            "v" -> {
                if (verificarValidade()) {
                    println("Valido")
                } else {
                    println("Invalido")
                }
            }
            "w" -> {
                val data = partes[1].toInt()
                val despesa = partes[2].toInt()
                val resultado = comportavel(data, despesa)
                if (resultado == null) {
                    println("Incomportavel")
                } else {
                    println(resultado)
                }
            }
            else -> println("Comando desconhecido")
        }
    }
}

fun inserir(partes: List<String>) {
    val montante = partes[1].toInt()
    val dataInicial = partes[2].toInt()
    val periodicidade = partes[3].toInt()
    val tipo = partes[4][0]
    val descricao = partes[5]

    var data = dataInicial

    while (data <= ULTIMA_DATA) {
        movimentos.add(Movimento(montante, data, tipo, descricao))
        if (periodicidade == 0) {
            break
        }
        data += periodicidade
    }
}

fun listar(data: Int) {
    val lista = movimentos.filter { it.data == data }

    for (i in lista.indices) {
        val movimento = lista[i]
        println("${i} ${movimento.montante} ${movimento.tipo} ${movimento.descricao}")
    }
}

fun remover(data: Int, indice: Int) {
    val lista = movimentos.filter { it.data == data }
    if (indice >= 0 && indice < lista.size) {
        movimentos.remove(lista[indice])
    }
}


fun calcularBalancos(): IntArray {
    val balancos = IntArray(1000)
    var saldo = 0

    for (data in 0..ULTIMA_DATA) {
        for (movimento in movimentos) {
            if (movimento.data == data) {
                if (movimento.tipo == 'r') {
                    saldo += movimento.montante
                } else {
                    saldo -= movimento.montante
                }
            }
        }

        balancos[data] = saldo
    }

    return balancos
}

fun balanco(data: Int): Int {
    return calcularBalancos()[data]
}

fun verificarValidade(): Boolean {
    val balancos = calcularBalancos()

    for (data in 0..ULTIMA_DATA) {
        if (balancos[data] < balancoMinimo) {
            return false
        }
    }

    return true
}

fun comportavel(dataInicial: Int, despesa: Int): Int? {
    val balancos = calcularBalancos()

    for (data in dataInicial..ULTIMA_DATA) {
        var pode = true

        // Verificar se o orçamento já era válido antes desta data.
        for (dia in 0 until data) {
            if (balancos[dia] < balancoMinimo) {
                pode = false
                break
            }
        }

        // Verificar se a nova despesa deixa o orçamento válido.
        if (pode) {
            for (dia in data..ULTIMA_DATA) {
                if (balancos[dia] - despesa < balancoMinimo) {
                    pode = false
                    break
                }
            }
        }

        if (pode) {
            return data
        }
    }

    return null
}