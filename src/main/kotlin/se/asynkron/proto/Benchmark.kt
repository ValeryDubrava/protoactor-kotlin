package se.asynkron.proto

import proto.actor.*
import proto.mailbox.Dispatchers
import proto.mailbox.ThreadPoolDispatcher
import java.lang.System.*
import java.util.concurrent.CountDownLatch

fun main(args: Array<String>){
    run()
    readLine()
}

fun run() {
    val messageCount: Int = 1_000_000
    val batchSize: Int = 500
    println("Dispatcher\t\tElapsed\t\tMsg/sec")
    val tps: Array<Int> = arrayOf(300, 400, 500, 600, 700, 800, 900)
    for (t in tps) {
        val d = ThreadPoolDispatcher()
        d.throughput = t
        
        val clientCount: Int = Runtime.getRuntime().availableProcessors() * 2

        val echoProps: Props = fromFunc({
            val tmp = message
            when (tmp) {
                is Msg -> {
                    val msg = tmp
                    msg.sender.tell(msg)
                }
            }
        }).withDispatcher(d)

        var pairs: List<Pair<PID,PID>> = listOf()
        val latch: CountDownLatch = CountDownLatch(clientCount)
        val clientProps: Props = fromProducer { PingActor(latch, messageCount, batchSize) }.withDispatcher(d)
        for (i in 0..clientCount) {
            pairs += Pair(spawn(clientProps),spawn(echoProps))
        }
        val sw: Long = currentTimeMillis()
        for ((client,echo) in pairs) {
            client.tell(Start(echo))
        }
        latch.await()

        val elapsedMillis = (currentTimeMillis() - sw).toDouble()
        val totalMessages: Int = messageCount * 2 * clientCount
        val x: Int = ((totalMessages.toDouble() / elapsedMillis * 1000.0).toInt())
        println("$t\t\t\t\t$elapsedMillis\t\t\t$x")
        for ((client,echo) in pairs) {
            client.stop()
            echo.stop()
        }

        Thread.sleep(500)
    }

}

class Msg(val sender: PID)
class Start(val sender: PID)

class PingActor(val latch: CountDownLatch, var messageCount: Int, val batchSize: Int, var batch: Int = 0) : Actor {
    suspend override fun receiveAsync(context: IContext) {
        val msg = context.message
        when (msg) {
            is Start -> sendBatch(context, msg.sender)
            is Msg -> {
                batch--
                if (batch > 0) return
                if (!sendBatch(context, msg.sender)) {
                    latch.countDown()
                }
            }
        }
    }

    private fun sendBatch(context: IContext, sender: PID): Boolean {
        when (messageCount) {
            0 -> {
                return false
            }
            else -> {
                val m: Msg = Msg(context.self)
                (0..batchSize).forEach { sender.tell(m) }
                messageCount -= batchSize
                batch = batchSize
                return true
            }
        }
    }
}

