package icu.mmmc.cr.tasks;

import icu.mmmc.cr.Cr;
import icu.mmmc.cr.PacketBody;
import icu.mmmc.cr.Version;
import icu.mmmc.cr.callbacks.ProgressCallback;
import icu.mmmc.cr.constants.NodeAttributes;
import icu.mmmc.cr.constants.TaskTypes;
import icu.mmmc.cr.database.DaoManager;
import icu.mmmc.cr.database.interfaces.NodeInfoDao;
import icu.mmmc.cr.entities.NodeInfo;
import icu.mmmc.cr.utils.BsonUtils;
import icu.mmmc.cr.utils.KeyUtils;
import icu.mmmc.cr.utils.Logger;
import org.bson.BSONObject;

import javax.crypto.Cipher;
import java.nio.charset.StandardCharsets;
import java.security.PublicKey;
import java.util.Arrays;
import java.util.Objects;
import java.util.Random;
import java.util.UUID;

/**
 * 初始化任务 被动
 *
 * @author shouchen
 */
@SuppressWarnings("DuplicatedCode")
public class InitTask0 extends AbstractTask {
    private int idCount;
    private PublicKey publicKey;
    private byte[] authCode;
    private NodeInfo nodeInfo;

    public InitTask0(ProgressCallback callback) {
        super(callback);
        idCount = 0;
    }

    /**
     * 处理包
     *
     * @param packetBody 包
     */
    @Override
    public void handlePacket(PacketBody packetBody) {
        super.handlePacket(packetBody);
        if (idCount == 0) {
            idCount = 1;
            BSONObject object = BsonUtils.deserialize(packetBody.getPayload());
            // 验证协议版本
            if (!Objects.equals(object.get("PROTOCOL"), Version.PATCH_VERSION)) {
                String s = "protocol version error";
                node.postPacket(new PacketBody()
                        .setSource(taskId)
                        .setDestination(packetBody.getSource())
                        .setTaskType(TaskTypes.ERROR)
                        .setPayload(s.getBytes(StandardCharsets.UTF_8)));
                halt(s);
            } else {
                // 发送公钥
                node.postPacket(new PacketBody()
                        .setSource(taskId)
                        .setDestination(packetBody.getSource())
                        .setPayload(Cr.getNodeInfo().getPublicKey().getEncoded()));
                // 拿到对方公钥
                byte[] pubKeyCode = (byte[]) object.get("PUB_KEY");
                try {
                    publicKey = KeyUtils.getPubKeyByCode(pubKeyCode);
                } catch (Exception e) {
                    Logger.warn(e);
                    halt(e.toString());
                    return;
                }
                // 解析uuid
                String uuid = UUID.nameUUIDFromBytes(pubKeyCode).toString();
                // 尝试从数据库获取节点信息
                NodeInfoDao dao = DaoManager.getNodeInfoDao();
                if (dao == null) {
                    nodeInfo = null;
                } else {
                    nodeInfo = dao.getByUUID(uuid);
                }
                if (nodeInfo == null) {
                    // 找不到该节点
                    if (Cr.CallBack.newConnectionCallback == null) {
                        halt("连接被拒绝");
                    } else {
                        // 询问用户是否允许连接
                        if (Cr.CallBack.newConnectionCallback.newConnection(uuid, true)) {
                            nodeInfo = new NodeInfo()
                                    .setUuid(uuid)
                                    .setPublicKey(publicKey);
                        } else {
                            halt("连接被拒绝");
                        }
                    }
                } else {
                    if (Cr.CallBack.newConnectionCallback != null) {
                        if (!Cr.CallBack.newConnectionCallback.newConnection(nodeInfo.getAttr(NodeAttributes.$NICK), false)) {
                            halt("连接被拒绝");
                        }
                    }
                }
            }
        } else if (idCount == 1) {
            idCount = 2;
            // 拿到加密后的AES密钥
            byte[] bytes = packetBody.getPayload();
            try {
                Cipher cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
                cipher.init(Cipher.DECRYPT_MODE, Cr.getPrivateKey());
                // 解密AES密钥
                bytes = cipher.doFinal(bytes);
                // 更新AES密钥
                node.getEncryptor().updateKey(KeyUtils.getAESKey(bytes));
                // 验证连接者身份
                Random random = new Random();
                // 生成随机验证码
                authCode = new byte[random.nextInt(64) + 64];
                random.nextBytes(authCode);
                cipher.init(Cipher.ENCRYPT_MODE, publicKey);
                node.postPacket(new PacketBody()
                        .setSource(taskId)
                        .setDestination(packetBody.getSource())
                        // 用对方公钥加密
                        .setPayload(cipher.doFinal(authCode)));
            } catch (Exception e) {
                Logger.warn(e);
                halt(e.toString());
            }
        } else if (idCount == 2) {
            byte[] bytes = packetBody.getPayload();
            if (Arrays.equals(bytes, authCode)) {
                node.postPacket(new PacketBody()
                        .setSource(taskId)
                        .setDestination(packetBody.getSource()));
                done();
            } else {
                String s = "身份验证失败";
                node.postPacket(new PacketBody()
                        .setSource(taskId)
                        .setDestination(packetBody.getSource())
                        .setTaskType(TaskTypes.ERROR)
                        .setPayload(s.getBytes(StandardCharsets.UTF_8)));
                halt(s);
            }
        } else {
            halt("");
        }
    }

    @Override
    public void halt(String msg) {
        super.halt(msg);
        try {
            node.initFail();
        } catch (Exception e) {
            Logger.warn(e);
        }
    }

    @Override
    public void done() {
        super.done();
        node.setNodeInfo(nodeInfo);
        node.initDone();
    }
}
