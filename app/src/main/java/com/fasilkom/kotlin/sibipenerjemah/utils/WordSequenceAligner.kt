package com.fasilkom.kotlin.sibipenerjemah.utils

import java.util.*

class WordSequenceAligner
/**
 * Konstruktor untuk inisasi kelas utama dan nilai-nilai pinalti pada proses "alignment".
 * Nilai-nilai pinalti dideklarasikan pada "companion object".
 */ @JvmOverloads constructor(
        // Pinalti dari "alignment" referensi dan hipotesis.
        private val substitutionPenalty: Int = DEFAULT_SUBSTITUTION_PENALTY,
        // Pinalti untuk pemasukkan kata yang tidak dibutuhkan.
        private val insertionPenalty: Int = DEFAULT_INSERTION_PENALTY,
        // Pinalti penghapusan referensi.
        private val deletionPenalty: Int = DEFAULT_DELETION_PENALTY) {

    inner class Alignment(reference: Array<String?>?, hypothesis: Array<String?>?, numSubstitutions: Int, numInsertions: Int, numDeletions: Int) {
        // Kalimat referensi atau kalimat yang benar.
        private val reference: Array<String?>

        // Kalimat hipotesis, kalimat yang akan diuji.
        private val hypothesis: Array<String?>

        // Jumlah substitusi atau pengurangan kata.
        val numSubstitutions: Int

        // Jumlah kata berlebihan.
        val numInsertions: Int

        // Jumlah kata hilang yang diperlukan.
        val numDeletions: Int

        // Jumlah kata benar yang ada pada hipotesis yang sesuai den referensi.
        val numCorrect: Int
            get() = hypothesisLength - (numSubstitutions + numInsertions) // Substitutions are mismatched and not correct, insertions are extra words that aren't correct

        // Digunakan untuk mencari panjang reference sequence
        val referenceLength: Int
            get() = reference.size - numInsertions

        //Digunakan untuk mendapatkan panjang hipotesis original.
        val hypothesisLength: Int
            get() = hypothesis.size - numDeletions

        // Konstruktor dari "inner class" alignment
        init {
            require(!(reference == null || hypothesis == null || reference.size != hypothesis.size || numSubstitutions < 0 || numInsertions < 0 || numDeletions < 0))
            this.reference = reference
            this.hypothesis = hypothesis
            this.numSubstitutions = numSubstitutions
            this.numInsertions = numInsertions
            this.numDeletions = numDeletions
        }
    }

    fun align(reference: Array<String>, hypothesis: Array<String>): Alignment {
        // Variabel-variabel berikut merepresentasikan operasi "edit" String pada matriks backtrace
        val OK = 0; val SUB = 1; val INS = 2; val DEL = 3

        /* Selanjutnya adalah tabel "dynamic programming" yang melacak perhitungan jarak "edit" String.
          * Alamat baris sesuai dengan indeks dalam urutan kata referensi.
          * Alamat kolom sesuai dengan indeks dalam urutan kata-kata hipotesis.
          * cost[0][0] membahas awal dari urutan dua kata, dan dengan demikian selalu memiliki biaya nol.
          * cost[3][2] adalah biaya "alignment" minimum saat "aligning" dua kata pertama dari referensi ke kata pertama hipotesis
        */
        val cost = Array(reference.size + 1) { IntArray(hypothesis.size + 1) }
        val backtrace = Array(reference.size + 1) { IntArray(hypothesis.size + 1) }

        cost[0][0] = 0
        backtrace[0][0] = OK

        for (i in 1 until cost.size) {
            cost[i][0] = deletionPenalty * i
            backtrace[i][0] = DEL
        }

        for (j in 1 until cost[0].size) {
            cost[0][j] = insertionPenalty * j
            backtrace[0][j] = INS
        }

        // "Loop" di bawah mencatat minimum cost "edit" operation
        for (i in 1 until cost.size) {
            for (j in 1 until cost[0].size) {
                var subOp: Int
                var cs: Int // Variabel ini digunakan untuk menentukan pemberian pinalti apabila hipotesis dan referensi tidak cocok.
                if (reference[i - 1].equals(hypothesis[j - 1], ignoreCase = false)) {
                    subOp = OK
                    cs = cost[i - 1][j - 1]
                } else {
                    subOp = SUB
                    cs = cost[i - 1][j - 1] + substitutionPenalty
                }
                val ci = cost[i][j - 1] + insertionPenalty
                val cd = cost[i - 1][j] + deletionPenalty
                val mincost = cs.coerceAtMost(ci.coerceAtMost(cd))
                if (cs == mincost) {
                    cost[i][j] = cs
                    backtrace[i][j] = subOp
                } else if (ci == mincost) {
                    cost[i][j] = ci
                    backtrace[i][j] = INS
                } else {
                    cost[i][j] = cd
                    backtrace[i][j] = DEL
                }
            }
        }

        // Setelah mendapatkan "mimimum costs", kita perlu mencari "cost" terkecil untuk "edit" hipotesis.
        val alignedReference = LinkedList<String?>()
        val alignedHypothesis = LinkedList<String?>()
        var numSub = 0
        var numDel = 0
        var numIns = 0
        var i = cost.size - 1
        var j: Int = cost[0].size - 1
        while (i > 0 || j > 0) {
            when (backtrace[i][j]) {
                OK -> {
                    alignedReference.add(0, reference[i - 1])
                    alignedHypothesis.add(0, hypothesis[j - 1])
                    i--
                    j--
                }
                SUB -> {
                    alignedReference.add(0, reference[i - 1])
                    alignedHypothesis.add(0, hypothesis[j - 1])
                    i--
                    j--
                    numSub++
                }
                INS -> {
                    alignedReference.add(0, null)
                    alignedHypothesis.add(0, hypothesis[j - 1])
                    j--
                    numIns++
                }
                DEL -> {
                    alignedReference.add(0, reference[i - 1])
                    alignedHypothesis.add(0, null)
                    i--
                    numDel++
                }
            }
        }
        return Alignment(alignedReference.toArray(arrayOf<String>()), alignedHypothesis.toArray(arrayOf<String>()), numSub, numIns, numDel)
    }

    // Fungsi digunakan untuk memberikan keluaran "Word Error Rate"
    fun wordErrorRate(reference: String, hypothesis: String): String {
        val wordErrorRateEval = WordSequenceAligner()
        val ref = reference.split(" ".toRegex()).toTypedArray()
        val hyp = hypothesis.split(" ".toRegex()).toTypedArray()
        val alignment: Alignment = wordErrorRateEval.align(ref, hyp)
        return "" + ((alignment.numSubstitutions + alignment.numInsertions + alignment.numDeletions) / alignment.referenceLength.toFloat()) * 100 + "%"
    }

    // Fungsi digunakan untuk memberikan keluaran "Sentence Accuracy"
    fun sentenceAccuracy(reference: String, hypothesis: String): String {
        val sentenceAlignerEval = WordSequenceAligner()
        val ref = reference.split(" ".toRegex()).toTypedArray()
        val hyp = hypothesis.split(" ".toRegex()).toTypedArray()
        val alignment: Alignment = sentenceAlignerEval.align(ref, hyp)
        return "" + (alignment.numCorrect / alignment.referenceLength.toFloat()) * 100 + "%"
    }

    companion object {
        // Biaya pinalti untuk operasi substitusi "edit" string pada saat "alignment".
        const val DEFAULT_SUBSTITUTION_PENALTY = 100

        // Biaya pinalti untuk operasi pemasukkan kata yang salah atau "redundant" pada saat "alignment".
        const val DEFAULT_INSERTION_PENALTY = 75

        // Biaya pinalti untuk operasi yang menghapus kata asli pada saat "alignment".
        const val DEFAULT_DELETION_PENALTY = 75
    }
}
