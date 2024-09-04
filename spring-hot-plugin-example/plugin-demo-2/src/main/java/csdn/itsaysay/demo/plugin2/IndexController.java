package csdn.itsaysay.demo.plugin2;

import com.alibaba.fastjson.JSON;
import csdn.itsaysay.demo.plugin2.bean.Person;
import csdn.itsaysay.demo.plugin2.bean.User;
import csdn.itsaysay.demo.plugin2.service.TestService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;

@Slf4j
@RestController
@RequestMapping("/demo2")
public class IndexController {

    @Resource
    private TestService testService;

    @GetMapping({"/hello"})
    public String hello() {
        return "Hello itsaysay!";
    }

    @GetMapping({"/hello/name"})
    public Person helloName() {
        Person person = new Person();
        person.setName("Human");
        String json = JSON.toJSONString(person);
        log.info(json);
        return person;
    }


    @GetMapping("/mybatis/getUser/{id}")
    public User getUser(@PathVariable("id") Integer id) {
        return testService.getUser(id);
    }

    @GetMapping("/mybatis/getUserXml/{id}")
    public User getUserXml(@PathVariable("id") Integer id) {
        return testService.getUserXml(id);
    }
}
