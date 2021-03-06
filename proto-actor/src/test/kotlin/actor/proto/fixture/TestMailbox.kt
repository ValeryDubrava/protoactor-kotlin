package actor.proto.fixture

import actor.proto.mailbox.Dispatcher
import actor.proto.mailbox.Mailbox
import actor.proto.mailbox.MessageInvoker
import actor.proto.mailbox.SystemMessage
import kotlinx.coroutines.runBlocking

class TestMailbox : Mailbox {
    private lateinit var _invoker: MessageInvoker
    private val userMessages: MutableList<Any> = mutableListOf()
    private val systemMessages: MutableList<Any> = mutableListOf()
    override fun postUserMessage(msg: Any) {
        userMessages.add(msg)
        runBlocking { _invoker.invokeUserMessage(msg) }
    }

    override fun postSystemMessage(msg: Any) {
        systemMessages.add(msg)
        runBlocking { _invoker.invokeSystemMessage(msg as SystemMessage) }
    }

    override fun registerHandlers(invoker: MessageInvoker, dispatcher: Dispatcher) {
        _invoker = invoker
    }

    override suspend fun run() {
    }

    override fun start() {}
}

