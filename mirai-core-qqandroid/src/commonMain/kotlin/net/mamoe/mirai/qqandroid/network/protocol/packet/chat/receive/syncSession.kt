/*
 *
 *  * Copyright 2020 Mamoe Technologies and contributors.
 *  *
 *  * 此源代码的使用受 GNU AFFERO GENERAL PUBLIC LICENSE version 3 许可证的约束, 可以在以下链接找到该许可证.
 *  * Use of this source code is governed by the GNU AGPLv3 license that can be found through the following link.
 *  *
 *  * https://github.com/mamoe/mirai/blob/master/LICENSE
 *
 */

package net.mamoe.mirai.qqandroid.network.protocol.packet.chat.receive

import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import net.mamoe.mirai.Bot
import net.mamoe.mirai.qqandroid.QQAndroidBot
import net.mamoe.mirai.qqandroid.network.protocol.data.proto.MsgSvc
import net.mamoe.mirai.qqandroid.network.protocol.packet.OutgoingPacket

internal fun Bot.startMessageSvcSync(
    syncCookie: ByteArray?,
): Job = (this as QQAndroidBot).network.run {
    launch(block = {
        class Data(
            var syncCookie: ByteArray?,
            var resp: MessageSvcPbGetMsg.Response
        )
        Data(
            syncCookie = syncCookie,
            resp = MessageSvcPbGetMsg(
                client,
                MsgSvc.SyncFlag.START,
                syncCookie
            ).sendAndExpect()
        ).run {
            val data = this
            flow {
                emit(handleResponse(this@startMessageSvcSync, resp, data.syncCookie))
            }.collect {
                data.syncCookie = resp.syncCookie
                resp = it?.sendAndExpect() ?: return@collect
            }
        }
    })
}

private fun handleResponse(
    bot: QQAndroidBot,
    packet: MessageSvcPbGetMsg.Response,
    syncCookie: ByteArray?
): OutgoingPacket? {
    val client = bot.client
    val network = bot.network
    when (packet.syncFlagFromServer) {
        MsgSvc.SyncFlag.STOP -> {

        }

        MsgSvc.SyncFlag.START -> {
            network.run {
                return MessageSvcPbGetMsg(
                    client,
                    MsgSvc.SyncFlag.CONTINUE,
                    syncCookie
                )
            }
        }

        MsgSvc.SyncFlag.CONTINUE -> {
            network.run {
                return MessageSvcPbGetMsg(
                    client,
                    MsgSvc.SyncFlag.CONTINUE,
                    syncCookie
                )
            }
        }
    }
    return null
}