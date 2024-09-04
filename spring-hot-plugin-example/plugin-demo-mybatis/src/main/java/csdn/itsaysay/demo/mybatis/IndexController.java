package csdn.itsaysay.demo.mybatis;

import csdn.itsaysay.demo.mybatis.bean.User;
import csdn.itsaysay.demo.mybatis.service.TestService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;

@Slf4j
@RestController
public class IndexController {

    @Resource
    private TestService testService;

    @GetMapping("/mybatis/getUser/{id}")
    public User getUser(@PathVariable("id") Integer id) {
        return testService.getUser(id);
    }

    @GetMapping("/mybatis/getUserXml/{id}")
    public User getUserXml(@PathVariable("id") Integer id) {
        return testService.getUserXml(id);
    }
}
