package com.resume.position.service;

import com.resume.dubbo.api.PositionService;
import com.resume.dubbo.domian.Position;
import com.resume.dubbo.domian.PositionTeam;
import com.resume.dubbo.domian.ResumeStateDTO;
import com.resume.position.mapper.PositionMapper;
import org.apache.dubbo.config.annotation.DubboService;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

/*
 *@filename: ResumeService
 *@author: lyh
 *@date:2023/7/8 21:43
 *@version 1.0
 *@description TODO
 */
@DubboService
public class ResumeService implements PositionService {
    @Autowired
    private PositionMapper positionMapper;

    @Autowired
    private PositionTeamService positionTeamService;

    @Override
    public int changePositionResumeCount(ResumeStateDTO resumeStateDTO) {
        return positionMapper.changePositionResumeCount(resumeStateDTO);
    }

    @Override
    public int addCandidateNum(Long positionId) {
        positionMapper.addCandidateNum(positionId);
        return positionMapper.getCandidateNum(positionId);
    }

    @Override
    public List<PositionTeam> queryOptionalInterviewer(Long positionId) {
        return positionTeamService.getSelectedPositionTeam(positionId);
    }

    @Override
    public Position getOne(Long positionId) {
        return positionMapper.selectById(positionId);
    }
}
