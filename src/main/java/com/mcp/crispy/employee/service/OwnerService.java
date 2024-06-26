package com.mcp.crispy.employee.service;

import com.mcp.crispy.auth.domain.EmployeePrincipal;
import com.mcp.crispy.common.ImageService;
import com.mcp.crispy.common.exception.EmployeeNotFoundException;
import com.mcp.crispy.email.service.EmailService;
import com.mcp.crispy.employee.dto.*;
import com.mcp.crispy.employee.mapper.EmployeeMapper;
import com.mcp.crispy.employee.mapper.OwnerMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static com.mcp.crispy.common.utils.RandomCodeUtils.generateTempPassword;

@Slf4j
@Service
@RequiredArgsConstructor
public class OwnerService {

    private final OwnerMapper ownerMapper;
    private final EmailService emailService;
    private final ImageService imageService;
    private final PasswordEncoder passwordEncoder;
    private final EmployeeMapper employeeMapper;


    // 직원 등록
    @Transactional
    public void registerEmployee(EmployeeRegisterDto employeeRegisterDto, Authentication authentication) {
        String tempPassword = generateTempPassword(); // 임시 비밀번호 생성
        String encodedPassword = passwordEncoder.encode(tempPassword); // 임시 비밀번호 암호화

        EmployeePrincipal userDetails = (EmployeePrincipal) authentication.getPrincipal();
        int frnNo = userDetails.getFrnNo(); // 프랜차이즈 번호 가져오기

        EmployeeRegisterDto employee = EmployeeRegisterDto.builder()
                .empId(employeeRegisterDto.getEmpId())
                .empPw(encodedPassword)
                .empName(employeeRegisterDto.getEmpName())
                .empEmail(employeeRegisterDto.getEmpEmail())
                .empPhone(employeeRegisterDto.getEmpPhone())
                .empStat(EmpStatus.EMPLOYED)
                .empInDt(employeeRegisterDto.getEmpInDt())
                .frnNo(frnNo)
                .posNo(Position.of(employeeRegisterDto.getPosNo().getCode()))
                .build();
        log.info("EmployeeRegisterDto: {}", employee); // DTO 객체 로깅
        employeeMapper.insertEmployee(employee); // 직원 정보 삽입

        emailService.sendTempPasswordEmail(employee.getEmpEmail(), tempPassword);
    }

    // 점주 등록
    @Transactional
    public int registerOwner(OwnerRegisterDto ownerRegisterDto, int frnNo, String frnOwner) {
        String tempPassword = generateTempPassword(); // 임시 비밀번호 생성
        String encodedPassword = passwordEncoder.encode(tempPassword); // 임시 비밀번호 암호화
        ownerRegisterDto.setEmpPw(encodedPassword);
        ownerRegisterDto.setFrnNo(frnNo);
        ownerRegisterDto.setEmpName(frnOwner);

        OwnerRegisterDto registerDto = OwnerRegisterDto.builder()
                .empId(ownerRegisterDto.getEmpId())
                .empPw(encodedPassword)
                .empName(ownerRegisterDto.getEmpName())
                .empEmail(ownerRegisterDto.getEmpEmail())
                .empPhone(ownerRegisterDto.getEmpPhone())
                .posNo(Position.OWNER.getCode())
                .frnNo(ownerRegisterDto.getFrnNo())
                .build();
        log.info("registerDto, frn: {}", frnNo);
        log.info("registerDto, posNo: {}", registerDto.getPosNo());
        ownerMapper.insertOwner(ownerRegisterDto); // 점주 정보 삽입
        emailService.sendTempPasswordEmail(ownerRegisterDto.getEmpEmail(), tempPassword);
        return registerDto.getEmpNo();
    }

    // 특정 가맹점에 속하는 직원 목록 조회
    public List<EmployeeDto> getEmployeesByFrnNo(int frnNo, Integer empStat,
                                                 Integer position, String empNameSearch) {

        EmpStatus status = empStat != null ? EmpStatus.of(empStat) : null;
        Position pos = position != null ? Position.of(position) : null;

        List<EmployeeDto> employeeByFranchise = ownerMapper.findEmployeeByFranchise(frnNo, status, pos, empNameSearch);
        log.info("employeeByFranchise: {}", employeeByFranchise);
        if(employeeByFranchise.isEmpty()) {
            throw new EmployeeNotFoundException("error", "아직 등록된 사원이 없습니다.");
        }
        return employeeByFranchise;
    }

    // 직원 삭제
    @Transactional
    public void removeEmployeeById(Integer empNo) {
        int count = ownerMapper.countByEmpNo(empNo);
        if (count > 0) {
            ownerMapper.deleteEmployee(empNo);
        } else {
            throw new IllegalArgumentException("해당하는 직원이 존재하지 않습니다.");
        }
    }

    // 직원 선택 삭제
    @Transactional
    public void removeEmployees(List<Integer> empNos) {
        log.info("선택된 직원: {}", empNos);
        ownerMapper.deleteEmployees(empNos);
    }
}
