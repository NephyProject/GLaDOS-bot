package jp.nephy.glados.api.plugin.twitter

import jp.nephy.glados.api.event.EventModel
import jp.nephy.glados.api.event.twitter.*

interface TwitterEventModel: EventModel {
    suspend fun onConnect(event: ConnectEvent) {}
    
    suspend fun onDisconnect(event: DisconnectEvent) {}
    
    suspend fun onStatus(event: StatusEvent) {}
    
    suspend fun onDirectMessage(event: DirectMessageEvent) {}
    
    suspend fun onFriends(event: FriendsEvent) {}
    
    suspend fun onDelete(event: DeleteEvent) {}
    
    suspend fun onHeartbeat(event: HeartbeatEvent) {}
    
    suspend fun onLength(event: LengthEvent) {}
    
    suspend fun onAnyJson(event: AnyJsonEvent) {}
    
    suspend fun onUnhandledJson(event: UnhandledJsonEvent) {}
    
    suspend fun onUnknownData(event: UnknownDataEvent) {}
    
    suspend fun onRawData(event: RawDataEvent) {}
}
