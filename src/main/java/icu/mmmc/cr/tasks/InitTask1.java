package icu.mmmc.cr.tasks;

import icu.mmmc.cr.Cr;
import icu.mmmc.cr.Node;
import icu.mmmc.cr.PacketBody;
import icu.mmmc.cr.Version;
import icu.mmmc.cr.callbacks.NewConnectionCallback;
import icu.mmmc.cr.callbacks.ProgressCallback;
import icu.mmmc.cr.constants.NodeAttributes;
import icu.mmmc.cr.constants.TaskTypes;
import icu.mmmc.cr.database.DaoManager;
import icu.mmmc.cr.entities.NodeInfo;
import icu.mmmc.cr.utils.BsonObject;
import icu.mmmc.cr.utils.KeyUtils;
import icu.mmmc.cr.utils.Logger;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import java.security.PublicKey;
import java.util.Objects;
import java.util.Random;
import java.util.UUID;

/**
 * 初始化任务 主动
 *
 * @author shouchen
 */
@SuppressWarnings("DuplicatedCode")
public class InitTask1 extends AbstractTask {
    private static final String RSA_CIPHER_ALGORITHM = "RSA/ECB/PKCS1Padding";
    private final String expectUUID;
    private int stepCount;
    private NodeInfo nodeInfo;

    public InitTask1(String uuid, ProgressCallback callback) {
        super(callback);
        expectUUID = uuid;
        stepCount = 0;
    }

    /**
     * 处理数据
     *
     * @param data 数据
     */
    @Override
    protected void handleData(byte[] data) throws Exception {
        if (stepCount == 0) {
            stepCount = 1;
            // 拿到公钥
            PublicKey publicKey;
            publicKey = KeyUtils.getPubKeyByCode(data);
            // 解析uuid
            String uuid = UUID.nameUUIDFromBytes(data).toString();
            if (expectUUID != null && !Objects.equals(expectUUID, uuid)) {
                halt("预期UUID:" + expectUUID + " 不匹配");
                return;
            }
            // 尝试从数据库获取节点信息
            nodeInfo = DaoManager.getNodeInfoDao().getByUUID(uuid);
            NewConnectionCallback newConnectionCallback = Cr.CallBack.newConnectionCallback;
            if (nodeInfo == null) {
                // 找不到该节点
                if (newConnectionCallback == null) {
                    halt("Connection refused");
                    return;
                } else {
                    // 询问用户是否允许连接
                    if (newConnectionCallback.newConnection(uuid, uuid, true)) {
                        // 生成一个临时节点信息
                        nodeInfo = new NodeInfo()
                                .setUUID(uuid)
                                .setPublicKey(publicKey);
                    } else {
                        halt("Connection refused");
                        return;
                    }
                }
            } else {
                if (newConnectionCallback != null) {
                    if (!newConnectionCallback.newConnection(uuid, nodeInfo.getAttr(NodeAttributes.$TITLE), false)) {
                        halt("Connection refused");
                        return;
                    }
                }
            }
            try {
                Cipher cipher = Cipher.getInstance(RSA_CIPHER_ALGORITHM);
                cipher.init(Cipher.ENCRYPT_MODE, publicKey);
                // 生成AES密钥
                SecretKey key = KeyUtils.genAESKey();
                byte[] bytes = new PacketBody()
                        .setSource(taskId)
                        .setDestination(destinationId)
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
                halt(e.toString());
            }
        } else if (stepCount == 1) {
            stepCount = 2;
            // 拿到加密后的验证码
            byte[] bytes = data;
            try {
                Cipher cipher = Cipher.getInstance(RSA_CIPHER_ALGORITHM);
                cipher.init(Cipher.DECRYPT_MODE, Cr.getPrivateKey());
                // 用自己的私钥解密
                bytes = cipher.doFinal(bytes);
                sendData(TaskTypes.ACK, bytes);
            } catch (Exception e) {
                Logger.warn(e);
                halt(e.toString());
            }
        } else if (stepCount == 2) {
            // 更新对方的信息
            nodeInfo = new NodeInfo(data);
            // 发送自己的信息
            sendData(TaskTypes.ACK, Cr.getNodeInfo().serialize());
            done();
        } else {
            halt("");
        }
    }

    @Override
    public void init(Node node, int taskId) {
        this.node = node;
        this.taskId = taskId;
        Random random = new Random();
        byte[] b = new byte[random.nextInt(1024) + 1];
        random.nextBytes(b);
        sendData(TaskTypes.INIT, new BsonObject()
                .set("", b)
                .set("PROTOCOL", Version.PROTOCOL_VERSION)
                .set("PUB_KEY",
                        Cr.getNodeInfo()
                                .getPublicKey()
                                .getEncoded()
                )
                .serialize());
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
