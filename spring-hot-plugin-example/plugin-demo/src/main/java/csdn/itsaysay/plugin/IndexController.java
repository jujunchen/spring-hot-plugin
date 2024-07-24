package csdn.itsaysay.plugin;

import com.alibaba.fastjson.JSON;
import csdn.itsaysay.plugin.bean.Person;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
public class IndexController {

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
}
