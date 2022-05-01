package icu.mmmc.cr.tasks;

import icu.mmmc.cr.Cr;
import icu.mmmc.cr.Node;
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
import org.bson.BasicBSONObject;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import java.security.PublicKey;
import java.util.Random;
import java.util.UUID;

/**
 * 初始化任务 主动
 *
 * @author shouchen
 */
@SuppressWarnings("DuplicatedCode")
public class InitTask1 extends AbstractTask {
    private int idCount;
    private NodeInfo nodeInfo;

    public InitTask1(ProgressCallback callback) {
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
            // 拿到公钥
            byte[] pubKeyCode = packetBody.getPayload();
            PublicKey publicKey;
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
                    return;
                } else {
                    // 询问用户是否允许连接
                    if (Cr.CallBack.newConnectionCallback.newConnection(uuid, true)) {
                        nodeInfo = new NodeInfo()
                                .setUuid(uuid)
                                .setPublicKey(publicKey);
                    } else {
                        halt("连接被拒绝");
                        return;
                    }
                }
            } else {
                if (Cr.CallBack.newConnectionCallback != null) {
                    if (!Cr.CallBack.newConnectionCallback.newConnection(nodeInfo.getAttr(NodeAttributes.$NICK), false)) {
                        halt("连接被拒绝");
                        return;
                    }
                }
            }
            try {
                Cipher cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
                cipher.init(Cipher.ENCRYPT_MODE, publicKey);
                // 生成AES密钥
                SecretKey key = KeyUtils.genAESKey();
                byte[] bytes = new PacketBody()
                        .setSource(taskId)
                        .setDestination(packetBody.getSource())
                        // 用RSA公钥加密
                        .setPayload(cipher.doFinal(key.getEncoded()))
                        .serialize();
                bytes = node.getEncryptor().encrypt(bytes);
                // 绕过队列直接写入
                node.doWrite(bytes);
                // 更新密钥
                node.getEncryptor().updateKey(key);
            } catch (Exception e) {
                Logger.warn(e);
                halt("");
            }
        } else if (idCount == 1) {
            idCount = 2;
            // 拿到加密后的验证码
            byte[] bytes = packetBody.getPayload();
            try {
                Cipher cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
                cipher.init(Cipher.DECRYPT_MODE, Cr.getPrivateKey());
                // 用自己的私钥解密
                bytes = cipher.doFinal(bytes);
                node.postPacket(new PacketBody()
                        .setSource(taskId)
                        .setDestination(packetBody.getSource())
                        .setPayload(bytes));
            } catch (Exception e) {
                Logger.warn(e);
                halt(e.toString());
            }
        } else if (idCount == 2) {
            done();
        } else {
            halt("");
        }
    }

    @Override
    public void init(Node node, int taskId) {
        this.node = node;
        this.taskId = taskId;
        BSONObject object = new BasicBSONObject();
        Random random = new Random();
        byte[] b = new byte[random.nextInt(127) + 1];
        random.nextBytes(b);
        object.put("", b);
        object.put("PROTOCOL", Version.PROTOCOL_VERSION);
        object.put("PUB_KEY", Cr.getNodeInfo().getPublicKey().getEncoded());
        node.postPacket(new PacketBody()
                .setSource(taskId)
                .setDestination(0)
                .setTaskType(TaskTypes.INIT)
                .setPayload(BsonUtils.serialize(object)));
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
        node.removeTask(taskId);
        node.setNodeInfo(nodeInfo);
        node.initDone();
    }
}
