package csdn.itsaysay.plugin.demo2.service;

import csdn.itsaysay.plugin.demo2.bean.User;

public interface TestService {
    User getUser(Integer id);

    User getUserXml(Integer id);
}
