package csdn.itsaysay.demo.plugin2.service;

import csdn.itsaysay.demo.plugin2.bean.User;

public interface TestService {
    User getUser(Integer id);

    User getUserXml(Integer id);
}
