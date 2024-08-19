package csdn.itsaysay.main.plugin2.service;

import csdn.itsaysay.main.plugin2.bean.User;

public interface TestService {
    User getUser(Integer id);

    User getUserXml(Integer id);
}
