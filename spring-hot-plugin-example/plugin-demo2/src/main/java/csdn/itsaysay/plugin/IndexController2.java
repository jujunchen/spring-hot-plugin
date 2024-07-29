package csdn.itsaysay.plugin;

import com.alibaba.fastjson.JSON;
import csdn.itsaysay.demo.plugin.service.PluginService;
import csdn.itsaysay.plugin.bean.Person;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;

@Slf4j
@RestController
public class IndexController2 {

    @Resource
    private PluginService pluginService;

    @GetMapping({"/hello2"})
    public String hello() {
        return "Hello itsaysay!";
    }

    @GetMapping({"/hello2/name"})
    public Person helloName() {
        Person person = new Person();
        person.setName("Human");
        String json = JSON.toJSONString(person);
        log.info(json);
        return person;
    }
}
