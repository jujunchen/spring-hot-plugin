package csdn.itsaysay.main.plugin2.bean;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("usertb")
public class User {

    @TableId
    private Integer id;

    private String name;

    private Integer age;
}
