package net.javaguides.employeeservice.service.impl;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.AllArgsConstructor;
import net.javaguides.employeeservice.dto.APIResponseDto;
import net.javaguides.employeeservice.dto.DepartmentDto;
import net.javaguides.employeeservice.dto.EmployeeDto;
import net.javaguides.employeeservice.dto.OrganizationDto;
import net.javaguides.employeeservice.entity.Employee;
import net.javaguides.employeeservice.mapper.EmployeeMapper;
import net.javaguides.employeeservice.repository.EmployeeRepository;
import net.javaguides.employeeservice.service.APIClient;
import net.javaguides.employeeservice.service.EmployeeService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.reactive.function.client.WebClient;

import static org.hibernate.sql.ast.SqlTreeCreationLogger.LOGGER;
import static org.springframework.boot.actuate.metrics.web.client.RestTemplateExchangeTags.uri;

@Service
@AllArgsConstructor
public class EmployeeServiceImpl implements EmployeeService {

    private EmployeeRepository employeeRepository;
   // private RestTemplate restTemplate;
    private WebClient webClient;
    private APIClient apiClient;
    private static Logger LOGGER= LoggerFactory.getLogger(EmployeeServiceImpl.class);

    @Override
    public EmployeeDto saveEmployee(EmployeeDto employeeDto) {

        Employee employee = EmployeeMapper.mapToEmployee(employeeDto);
        Employee saveEmployee=employeeRepository.save(employee);

        EmployeeDto savedEmployeeDto= EmployeeMapper.mapToEmployeeDto(employee);
        return savedEmployeeDto;
    }

   // @CircuitBreaker(name = "{$spring.application.name}", fallbackMethod = "getDefaultDepartment")
    @Retry(name = "${spring.application.name}", fallbackMethod = "getDefaultDepartment")
    @Override
    public APIResponseDto getEmployeeById(Long employeeId) {

        LOGGER.info("Inside getEmployeeById() method");
        Employee employee = employeeRepository.findById(employeeId).get();

       /* ResponseEntity<DepartmentDto> responseEntity= restTemplate.getForEntity("http://localhost:8080/api/departments/" +employee.getDepartmentCode(), DepartmentDto.class);
        DepartmentDto departmentDto= responseEntity.getBody();*/

       DepartmentDto departmentDto= webClient.get()
               .uri("http://localhost:8082/api/departments/"+employee.getDepartmentCode())
                .retrieve()
                .bodyToMono(DepartmentDto.class)
                .block();
   //     DepartmentDto departmentDto = apiClient.getDepartment(employee.getDepartmentCode());

        OrganizationDto organizationDto = webClient.get()
                .uri("http://localhost:8083/api/organizations/" + employee.getOrganizationCode())
                .retrieve()
                .bodyToMono(OrganizationDto.class)
                .block();


        EmployeeDto employeeDto= EmployeeMapper.mapToEmployeeDto(employee);

        APIResponseDto apiResponseDto= new APIResponseDto();

        apiResponseDto.setEmployeeDto(employeeDto);
        apiResponseDto.setDepartmentDto(departmentDto);
        apiResponseDto.setOrganizationDto(organizationDto);

        return apiResponseDto;
    }

    public APIResponseDto getDefaultDepartment(Long employeeId, Exception exception) {

        Employee employee = employeeRepository.findById(employeeId).get();
        LOGGER.info("inside getDefaultDepartment() method");

        DepartmentDto departmentDto = new DepartmentDto();
        departmentDto.setDepartmentName("R&D Department");
        departmentDto.setDepartmentCode("RD001");
        departmentDto.setDepartmentDescription("Research and Development Department");

        EmployeeDto employeeDto= EmployeeMapper.mapToEmployeeDto(employee);

        APIResponseDto apiResponseDto= new APIResponseDto();

        apiResponseDto.setEmployeeDto(employeeDto);
        apiResponseDto.setDepartmentDto(departmentDto);

        return apiResponseDto;
    }
}
