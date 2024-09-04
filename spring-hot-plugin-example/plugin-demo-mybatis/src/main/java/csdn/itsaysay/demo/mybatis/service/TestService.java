package csdn.itsaysay.demo.mybatis.service;

import csdn.itsaysay.demo.mybatis.bean.User;

public interface TestService {
    User getUser(Integer id);

    User getUserXml(Integer id);
}
