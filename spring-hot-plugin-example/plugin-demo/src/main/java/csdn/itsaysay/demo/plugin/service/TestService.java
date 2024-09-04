package csdn.itsaysay.demo.plugin.service;

import csdn.itsaysay.demo.plugin.bean.User;

public interface TestService {
    User getUser(Integer id);

    User getUserXml(Integer id);
}
