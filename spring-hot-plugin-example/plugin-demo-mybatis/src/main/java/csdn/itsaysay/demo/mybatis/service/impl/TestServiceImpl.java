package csdn.itsaysay.demo.mybatis.service.impl;

import csdn.itsaysay.demo.mybatis.bean.User;
import csdn.itsaysay.demo.mybatis.mapper.TestMapper;
import csdn.itsaysay.demo.mybatis.service.TestService;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

@Service
public class TestServiceImpl implements TestService {

    @Resource
    private TestMapper testMapper;

    @Override
    public User getUser(Integer id) {
        return testMapper.selectById(id);
    }

    @Override
    public User getUserXml(Integer id) {
        return testMapper.selectByIdXml(id);
    }
}
