package me.earth.earthhack.impl.core.mixins.network;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import me.earth.earthhack.api.cache.ModuleCache;
import me.earth.earthhack.impl.Duny4hil3;
import me.earth.earthhack.impl.modules.Caches;
import me.earth.earthhack.impl.modules.misc.logger.Logger;
import me.earth.earthhack.impl.modules.misc.logger.util.LoggerMode;
import net.minecraft.network.EnumPacketDirection;
import net.minecraft.network.NettyPacketEncoder;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.Packet;
import net.minecraft.network.PacketBuffer;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(NettyPacketEncoder.class)
public abstract class MixinNettyPacketEncoder
{
    private static final ModuleCache<Logger> LOGGER_MODULE =
            Caches.getModule(Logger.class);

    @Shadow
    @Final
    private EnumPacketDirection direction;

    @Inject(
        method = "encode",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/network/Packet;writePacketData(Lnet/minecraft/network/PacketBuffer;)V",
            shift = At.Shift.AFTER))
    private void encodeHook(ChannelHandlerContext p_encode_1_,
                            Packet<?> p_encode_2_,
                            ByteBuf p_encode_3_,
                            CallbackInfo ci)
    {
        if (this.direction == EnumPacketDirection.SERVERBOUND
            && LOGGER_MODULE.isEnabled()
            && LOGGER_MODULE.get().getMode() == LoggerMode.Buffer)
        {
            if (p_encode_3_.readableBytes() != 0)
            {
                int writerIndex = p_encode_3_.writerIndex();
                int readerIndex = p_encode_3_.readerIndex();

                PacketBuffer packetbuffer = new PacketBuffer(p_encode_3_);

                try
                {
                    int i = packetbuffer.readVarInt();
                    Packet<?> packet =
                            p_encode_1_
                                .channel()
                                .attr(NetworkManager.PROTOCOL_ATTRIBUTE_KEY)
                                .get()
                                .getPacket(EnumPacketDirection.SERVERBOUND, i);

                    if (packet != null)
                    {
                        packet.readPacketData(packetbuffer);
                        if (packetbuffer.readableBytes() > 0)
                        {
                            Duny4hil3.getLogger().warn("Packet: "
                                + packet.getClass().getName()
                                + " : " + p_encode_2_.getClass().getName()
                                + " has leftover bytes in the PacketBuffer!");
                        }

                        LOGGER_MODULE.get().logPacket(packet,
                                "Originally: " + p_encode_2_.getClass()
                                                            .getName()
                                + ", ",
                                false);
                    }
                    else
                    {
                        Duny4hil3.getLogger().warn(
                                "Packet was null for id: " + i);
                    }
                }
                catch (Exception e)
                {
                    e.printStackTrace();
                }

                p_encode_3_.resetReaderIndex();
                if (p_encode_3_.readerIndex() != readerIndex
                        || p_encode_3_.writerIndex() != writerIndex)
                {
                    Duny4hil3.getLogger().error(
                        "Indices are not matching for packet: "
                            + p_encode_2_.getClass().getName()
                            + "! ReaderIndex: "
                            + readerIndex + ", now: "
                            + p_encode_3_.readerIndex()
                            + ", WriterIndex: " + writerIndex
                            + ", now: "
                            + p_encode_3_.writerIndex());
                }
            }
            else
            {
                Duny4hil3.getLogger()
                         .warn("Packet "
                                 + p_encode_2_.getClass().getName()
                                 + " has no readable bytes!");
            }
        }
    }

}
