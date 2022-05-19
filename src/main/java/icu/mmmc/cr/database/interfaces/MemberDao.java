package icu.mmmc.cr.database.interfaces;

import icu.mmmc.cr.entities.MemberInfo;

import java.util.List;

/**
 * 房间成员数据访问接口
 *
 * @author shouchen
 */
@SuppressWarnings("unused")
public interface MemberDao {
    /**
     * 从数据库删除一个成员信息
     *
     * @param memberInfo 成员信息
     */
    void deleteMember(MemberInfo memberInfo);

    /**
     * 更新或添加成员信息
     *
     * @param memberInfo 成员信息
     */
    void updateMember(MemberInfo memberInfo);

    /**
     * 获取指定房间的成员列表
     *
     * @param nodeUUID 节点标识码
     * @param roomUUID 房间标识码
     * @return 成员列表
     */
    List<MemberInfo> getMemberList(String nodeUUID, String roomUUID);
}
