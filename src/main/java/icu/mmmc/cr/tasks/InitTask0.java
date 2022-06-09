package icu.mmmc.cr.tasks;

import icu.mmmc.cr.Cr;
import icu.mmmc.cr.Version;
import icu.mmmc.cr.callbacks.NewConnectionCallback;
import icu.mmmc.cr.constants.NodeAttributes;
import icu.mmmc.cr.constants.TaskTypes;
import icu.mmmc.cr.database.DaoManager;
import icu.mmmc.cr.entities.NodeInfo;
import icu.mmmc.cr.utils.BsonUtils;
import icu.mmmc.cr.utils.KeyUtils;
import icu.mmmc.cr.utils.Logger;
import org.bson.BSONObject;

import javax.crypto.Cipher;
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
    private static final String RSA_CIPHER_ALGORITHM = "RSA/ECB/PKCS1Padding";
    private int stepCount;
    private PublicKey publicKey;
    private byte[] authCode;
    private NodeInfo nodeInfo;

    public InitTask0() {
        super(null);
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
            BSONObject object = BsonUtils.deserialize(data);
            // 验证协议版本
            if (!Objects.equals(object.get("PROTOCOL"), Version.PROTOCOL_VERSION)) {
                String s = "protocol version error";
                sendError(s);
                halt(s);
            } else {
                // 发送公钥
                sendData(TaskTypes.ACK, Cr.getNodeInfo().getPublicKey().getEncoded());
                // 拿到对方公钥
                byte[] pubKeyCode = (byte[]) object.get("PUB_KEY");
                publicKey = KeyUtils.getPubKeyByCode(pubKeyCode);
                // 解析uuid
                String uuid = UUID.nameUUIDFromBytes(pubKeyCode).toString();
                // 尝试从数据库获取节点信息
                nodeInfo = DaoManager.getNodeInfoDao().getByUUID(uuid);
                NewConnectionCallback newConnectionCallback = Cr.CallBack.newConnectionCallback;
                if (nodeInfo == null) {
                    // 找不到该节点
                    if (newConnectionCallback == null) {
                        halt("连接被拒绝");
                    } else {
                        // 询问用户是否允许连接
                        if (newConnectionCallback.newConnection(uuid, uuid, true)) {
                            // 生成一个临时节点信息
                            nodeInfo = new NodeInfo()
                                    .setUUID(uuid)
                                    .setPublicKey(publicKey);
                        } else {
                            halt("连接被拒绝");
                        }
                    }
                } else {
                    if (newConnectionCallback != null) {
                        if (!newConnectionCallback.newConnection(uuid, nodeInfo.getAttr(NodeAttributes.$TITLE), false)) {
                            halt("连接被拒绝");
                        }
                    }
                }
            }
        } else if (stepCount == 1) {
            stepCount = 2;
            // 拿到加密后的AES密钥
            byte[] bytes = data;
            Cipher cipher = Cipher.getInstance(RSA_CIPHER_ALGORITHM);
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
            // 用对方公钥初始化加密器
            cipher.init(Cipher.ENCRYPT_MODE, publicKey);
            // 用对方公钥加密并发送验证码
            sendData(TaskTypes.ACK, cipher.doFinal(authCode));
        } else if (stepCount == 2) {
            stepCount = 3;
            // 验证对方发来的验证码
            if (Arrays.equals(data, authCode)) {
                // 验证通过，发送自己的节点信息
                sendData(TaskTypes.ACK, Cr.getNodeInfo().serialize());
            } else {
                String s = "身份验证失败";
                sendError(s);
                halt(s);
            }
        } else if (stepCount == 3) {
            nodeInfo = new NodeInfo(data);
            done();
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
