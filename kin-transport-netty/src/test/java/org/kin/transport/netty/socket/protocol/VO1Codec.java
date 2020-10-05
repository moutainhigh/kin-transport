package org.kin.transport.netty.socket.protocol;

/**
 * @author huangjianqin
 * @date 2020/10/6
 */
public class VO1Codec extends ProtocolCodecAdapter<VO1> {
    @Override
    public VO1 readVO(SocketRequestOprs request) {
        return VO1.of(request.readInt());
    }

    @Override
    public void writeVO(VO1 vo, SocketResponseOprs response) {
        response.writeInt(vo.getId());
    }
}
