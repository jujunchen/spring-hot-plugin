package csdn.itsaysay.demo.mybatis.mapper;

import csdn.itsaysay.demo.mybatis.bean.User;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface TestMapper {

    @Select("select * from usertb where id = #{id}")
    User selectById(@Param("id") Integer id);

    User selectByIdXml(@Param("id") Integer id);
}
