package icu.mmmc.cr.database.adapters;

import icu.mmmc.cr.database.interfaces.MemberDao;
import icu.mmmc.cr.entities.MemberInfo;

import java.util.ArrayList;
import java.util.List;

/**
 * 房间成员数据访问适配器
 *
 * @author shouchen
 */
@SuppressWarnings("unused")
public class MemberDaoAdapter implements MemberDao {
    /**
     * 从数据库删除一个成员信息
     *
     * @param memberInfo 成员信息
     */
    @Override
    public void deleteMember(MemberInfo memberInfo) {
    }

    /**
     * 更新或添加成员信息
     *
     * @param memberInfo 成员信息
     */
    @Override
    public void updateMember(MemberInfo memberInfo) {
    }

    /**
     * 获取指定房间的成员列表
     *
     * @param nodeUUID 节点标识码
     * @param roomUUID 房间标识码
     * @return 成员列表
     */
    @Override
    public List<MemberInfo> getMemberList(String nodeUUID, String roomUUID) {
        return new ArrayList<>();
    }
}
