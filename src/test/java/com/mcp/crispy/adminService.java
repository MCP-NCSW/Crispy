package com.mcp.crispy;

import com.mcp.crispy.admin.dto.AdminDto;
import com.mcp.crispy.admin.mapper.AdminMapper;
import com.mcp.crispy.employee.dto.EmpStatus;
import com.mcp.crispy.employee.dto.EmployeeDto;
import com.mcp.crispy.employee.dto.Position;
import com.mcp.crispy.employee.mapper.EmployeeMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
public class adminService {

    @Autowired
    private EmployeeMapper employeeMapper;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    void 직원_등록() {
        String password = "1234";
        String encoded = passwordEncoder.encode(password);
        EmployeeDto employeeDto = EmployeeDto.builder()
                .empId("moz1mozi")
                .empPw(encoded)
                .empEmail("moz1mozi@gmail.com")
                .empName("모지히")
                .empPhone("010-1234-1234")
                .empStat(EmpStatus.EMPLOYED)
                .posNo(Position.EMPLOYEE)
                .build();
        employeeMapper.insertEmployee(employeeDto);
    }

}