package csdn.itsaysay.main.plugin2.service.impl;

import csdn.itsaysay.main.plugin2.bean.User;
import csdn.itsaysay.main.plugin2.service.TestService;
import csdn.itsaysay.main.plugin2.mapper.TestMapper;
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
