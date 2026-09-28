package dev.patrickgold.florisboard.repli.voice

/** Main-thread, single-driver gate: share an in-flight non-recording preflight with one explicit start. */
internal class VoicePreparationGate {
    private var preparing = false
    private var prepared = false
    private var closed = false
    private var started = false
    private var start: (() -> Unit)? = null

    fun prepare(query: () -> Unit) {
        if (closed || preparing || prepared) return
        preparing = true
        query()
    }

    fun listen(query: () -> Unit, action: () -> Unit) {
        if (closed || start != null || started) return
        start = action
        prepare(query)
        dispatch()
    }

    fun complete() { if (!closed) { prepared = true; dispatch() } }
    fun close() { closed = true; start = null }
    private fun dispatch() {
        if (prepared && !closed) {
            val action = start
            start = null
            if (action != null) { started = true; action() }
        }
    }
}
