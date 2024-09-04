package csdn.itsaysay.demo.plugin.service.impl;

import csdn.itsaysay.demo.plugin.bean.User;
import csdn.itsaysay.demo.plugin.mapper.TestMapper;
import csdn.itsaysay.demo.plugin.mapper.TestMapper2;
import csdn.itsaysay.demo.plugin.service.TestService;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

@Service
public class TestServiceImpl implements TestService {

    @Resource
    private TestMapper testMapper;
    @Resource
    private TestMapper2 testMapper2;


    @Override
    public User getUser(Integer id) {
        return testMapper.selectById(id);
    }

    @Override
    public User getUserXml(Integer id) {
        return testMapper.selectByIdXml(id);
    }
}
