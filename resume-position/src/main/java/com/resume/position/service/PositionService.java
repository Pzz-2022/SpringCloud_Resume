package com.resume.position.service;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.resume.base.model.PageBean;
import com.resume.base.model.TokenInfo;
import com.resume.base.utils.Constant;
import com.resume.dubbo.domian.Position;
import com.resume.position.mapper.PositionMapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.resume.position.utils.CacheClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

/**
 * <p>
 * 服务实现类
 * </p>
 *
 * @author 彭政
 * @since 2023-06-18
 */
@Service
public class PositionService extends ServiceImpl<PositionMapper, Position> {

    @Autowired
    private CacheClient cacheClient;

    @Resource
    private PositionMapper positionMapper;

//    public Position getOne(Long companyId, Long positionId) {
//        // 解决缓存穿透
////        Position position = cacheClient.queryWithPassThrough(CACHE_POSITION_KEY, positionId, Position.class, this::getById, CACHE_POSITION_TTL);
//        // 利用互斥锁解决缓存击穿
//        Position position = cacheClient.queryWithMutex(CACHE_POSITION_KEY, positionId, Position.class, this::getById, CACHE_POSITION_TTL);
//        // 利用逻辑过期解决缓存击穿
////        Position position = cacheClient.queryWithLogicalExpire(CACHE_POSITION_KEY, positionId, Position.class, this::getById, CACHE_POSITION_TTL);
//        return position;
//    }

    public PageBean<Position> selectPositionByPage(TokenInfo tokenInfo, int nowPage) {
        PageBean<Position> pageBean = new PageBean<>();
        Long userId = tokenInfo.getPkUserId();
        Long companyId = tokenInfo.getCompanyId();
        int offset = (nowPage - 1) * Constant.PAGE_SIZE;
        switch (tokenInfo.getRole()) {
            case Constant.COMPANY_ADMIN:
                pageBean.setData(positionMapper.selectPositionByAdmin(companyId, offset, Constant.PAGE_SIZE));
                pageBean.setTotalCount(positionMapper.totalCountPositionByAdmin(companyId));
                break;
            case Constant.HR:
                pageBean.setData(positionMapper.selectPositionByHr(userId, offset, Constant.PAGE_SIZE));
                pageBean.setTotalCount(positionMapper.totalCountPositionByHr(userId));
                break;
            case Constant.INTERVIEWER:
                pageBean.setData(positionMapper.selectPositionByInterviewer(userId, offset, Constant.PAGE_SIZE));
                pageBean.setTotalCount(positionMapper.totalCountPositionByInterviewer(userId));
                break;
        }
        if (pageBean.getTotalCount() % Constant.PAGE_SIZE == 0)
            pageBean.setTotalPage(pageBean.getTotalCount() / Constant.PAGE_SIZE);
        else pageBean.setTotalPage(pageBean.getTotalCount() / Constant.PAGE_SIZE + 1);
        pageBean.setNowPage(nowPage);
        return pageBean;
    }

    public boolean editPosition(Position position) {
        LambdaUpdateWrapper<Position> lambdaUpdateWrapper = new LambdaUpdateWrapper<>();
        lambdaUpdateWrapper.eq(Position::getPkPositionId,position.getPkPositionId());
        return update(position,lambdaUpdateWrapper);
    }
    public boolean closePosition(Position position) {
        LambdaUpdateWrapper<Position> lambdaUpdateWrapper = new LambdaUpdateWrapper<>();
        lambdaUpdateWrapper.eq(Position::getPkPositionId,position.getPkPositionId());
        lambdaUpdateWrapper.set(Position::getState,0);
        return update(lambdaUpdateWrapper);
    }
    public boolean openPosition(Position position) {
        LambdaUpdateWrapper<Position> lambdaUpdateWrapper = new LambdaUpdateWrapper<>();
        lambdaUpdateWrapper.eq(Position::getPkPositionId,position.getPkPositionId());
        lambdaUpdateWrapper.set(Position::getState,1);
        return update(lambdaUpdateWrapper);
    }
}