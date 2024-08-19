package csdn.itsaysay.main.plugin.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import csdn.itsaysay.main.plugin.bean.User;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface TestMapper extends BaseMapper<User> {

    User selectByIdXml(@Param("id") Integer id);
}
