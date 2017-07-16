package proto.remote

import actor.proto.*
import actor.proto.remote.EndpointTerminatedEvent
import actor.proto.remote.RemoteTerminate
import actor.proto.remote.RemoteUnwatch
import actor.proto.remote.RemoteWatch

class EndpointWatcher(address: String) : Actor {
    private val _behavior : Behavior
    private val _watched : HashMap<String, PID> = HashMap<String, PID>()
    private var _address : String? = address
    suspend override fun receiveAsync (context : Context) {
        return _behavior.receiveAsync(context)
    }
    private suspend fun connectedAsync (context : Context) {
        val msg = context.message
        when (msg) {
            is RemoteTerminate -> {
                _watched.remove(msg.watcher.id)
                val t = Terminated(msg.watchee,true)
                msg.watcher.sendSystemMessage(t)
            }
            is EndpointTerminatedEvent -> {
                for ((id, pid) in _watched) {
                    val t = Terminated(pid,true)
                    val watcher : PID = PID(ProcessRegistry.address, id)
                    watcher.sendSystemMessage(t)
                }
                _behavior.become({terminatedAsync(it)})
            }
            is RemoteUnwatch -> {
                _watched.remove(msg.watcher.id)
                val w : Unwatch = Unwatch(msg.watcher)
                Remote.sendMessage(msg.watchee, w, -1)
            }
            is RemoteWatch -> {
                _watched.put(msg.watcher.id, msg.watchee)
                val w : Watch = Watch(msg.watcher)
                Remote.sendMessage(msg.watchee, w, -1)
            }
        }
    }
    private suspend fun terminatedAsync (context : Context) {
        val msg = context.message
        when (msg) {
            is RemoteWatch -> msg.watcher.sendSystemMessage(Terminated(msg.watchee,true))
            is RemoteUnwatch,
            is EndpointTerminatedEvent,
            is RemoteTerminate -> {
            }
            else -> {
            }
        }
    }

    init {
        _behavior = Behavior({ connectedAsync (it)})
    }
}
