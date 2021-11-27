package com.kele.rpc.codec;

import com.kele.rpc.ProtoStuffUtils;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;

/**
 * @author icanner
 * @date 2021/11/27:10:38 下午
 */
public class RpcEncoder extends MessageToByteEncoder<Object> {
    
    private Class<?> genericClass;
    
    public RpcEncoder(Class<?> genericClass) {
        this.genericClass = genericClass;
    }
    
    @Override
    protected void encode(ChannelHandlerContext ctx, Object msg, ByteBuf out) throws Exception {
        if (genericClass.isInstance(msg)) {
            byte[] data = ProtoStuffUtils.serialize(msg);
            // 消息头(数据包长度) + 消息体（数据包内容）
            out.writeInt(data.length);
            out.writeBytes(data);
        }
    }
}
