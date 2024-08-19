package csdn.itsaysay.main.plugin.service;

import csdn.itsaysay.main.plugin.bean.User;

public interface TestService {
    User getUser(Integer id);

    User getUserXml(Integer id);
}
