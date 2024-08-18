package csdn.itsaysay.plugin.demo2.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import csdn.itsaysay.plugin.demo2.bean.User;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface TestMapper extends BaseMapper<User> {

    User selectByIdXml(@Param("id") Integer id);
}
