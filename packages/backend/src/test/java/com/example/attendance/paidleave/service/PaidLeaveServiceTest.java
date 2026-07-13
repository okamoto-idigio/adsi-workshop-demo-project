package com.example.attendance.paidleave.service;

import com.example.attendance.attendance.entity.AttendanceRecord;
import com.example.attendance.attendance.repository.AttendanceRecordRepository;
import com.example.attendance.department.entity.Department;
import com.example.attendance.employee.entity.Employee;
import com.example.attendance.employee.entity.Role;
import com.example.attendance.employee.repository.EmployeeRepository;
import com.example.attendance.paidleave.dto.PaidLeaveCreateRequest;
import com.example.attendance.paidleave.entity.LeaveRequestStatus;
import com.example.attendance.paidleave.entity.LeaveType;
import com.example.attendance.paidleave.entity.PaidLeaveRequest;
import com.example.attendance.paidleave.repository.PaidLeaveRequestRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaidLeaveServiceTest {

    @Mock private PaidLeaveRequestRepository leaveRepository;
    @Mock private EmployeeRepository employeeRepository;
    @Mock private AttendanceRecordRepository attendanceRecordRepository;

    private PaidLeaveServiceImpl service;
    private Department department;
    private Employee employee;
    private Employee manager;

    @BeforeEach
    void setUp() {
        Clock fixedClock = Clock.fixed(Instant.parse("2026-07-13T00:00:00Z"), ZoneId.of("Asia/Tokyo"));
        service = new PaidLeaveServiceImpl(leaveRepository, employeeRepository, attendanceRecordRepository, fixedClock);

        department = Department.builder().id(UUID.randomUUID()).name("Engineering").build();
        employee = Employee.builder().id(UUID.randomUUID()).name("田中太郎").email("tanaka@example.com")
                .password("hashed").department(department).role(Role.EMPLOYEE)
                .isManager(false).hireDate(LocalDate.of(2024, 4, 1)).build();
        manager = Employee.builder().id(UUID.randomUUID()).name("佐藤次郎").email("sato@example.com")
                .password("hashed").department(department).role(Role.EMPLOYEE)
                .isManager(true).hireDate(LocalDate.of(2020, 4, 1)).build();
    }

    @Nested @DisplayName("有給申請作成") class Create {
        @Test @DisplayName("全休申請（1日）が正常に作成される")
        void create_fullDaySingle() {
            var req = new PaidLeaveCreateRequest(LocalDate.of(2026,7,14), LocalDate.of(2026,7,14), LeaveType.FULL_DAY);
            when(employeeRepository.findById(employee.getId())).thenReturn(Optional.of(employee));
            when(leaveRepository.findOverlapping(any(),any(),any(),any())).thenReturn(List.of());
            when(attendanceRecordRepository.findByEmployeeIdAndWorkDateIn(any(),any())).thenReturn(List.of());
            when(leaveRepository.save(any(PaidLeaveRequest.class))).thenAnswer(i -> i.getArgument(0));
            var result = service.create(employee.getId(), req);
            assertThat(result.status()).isEqualTo(LeaveRequestStatus.PENDING);
            assertThat(result.leaveDays()).isEqualByComparingTo("1.0");
        }
        @Test @DisplayName("連続5日（土日含む7日間）が土日スキップで5日")
        void create_skipsWeekends() {
            var req = new PaidLeaveCreateRequest(LocalDate.of(2026,7,20), LocalDate.of(2026,7,26), LeaveType.FULL_DAY);
            when(employeeRepository.findById(employee.getId())).thenReturn(Optional.of(employee));
            when(leaveRepository.findOverlapping(any(),any(),any(),any())).thenReturn(List.of());
            when(attendanceRecordRepository.findByEmployeeIdAndWorkDateIn(any(),any())).thenReturn(List.of());
            when(leaveRepository.save(any(PaidLeaveRequest.class))).thenAnswer(i -> i.getArgument(0));
            assertThat(service.create(employee.getId(), req).leaveDays()).isEqualByComparingTo("5.0");
        }
        @Test @DisplayName("午前休申請が0.5日で作成")
        void create_amHalf() {
            var req = new PaidLeaveCreateRequest(LocalDate.of(2026,7,14), LocalDate.of(2026,7,14), LeaveType.AM_HALF);
            when(employeeRepository.findById(employee.getId())).thenReturn(Optional.of(employee));
            when(leaveRepository.findOverlapping(any(),any(),any(),any())).thenReturn(List.of());
            when(attendanceRecordRepository.findByEmployeeIdAndWorkDateIn(any(),any())).thenReturn(List.of());
            when(leaveRepository.save(any(PaidLeaveRequest.class))).thenAnswer(i -> i.getArgument(0));
            assertThat(service.create(employee.getId(), req).leaveDays()).isEqualByComparingTo("0.5");
        }
        @Test @DisplayName("半休で連続申請→400")
        void create_halfMultiDay_400() {
            var req = new PaidLeaveCreateRequest(LocalDate.of(2026,7,14), LocalDate.of(2026,7,15), LeaveType.AM_HALF);
            when(employeeRepository.findById(employee.getId())).thenReturn(Optional.of(employee));
            assertThatThrownBy(() -> service.create(employee.getId(), req))
                    .isInstanceOf(ResponseStatusException.class).hasMessageContaining("400");
        }
        @Test @DisplayName("過去日→400")
        void create_pastDate_400() {
            var req = new PaidLeaveCreateRequest(LocalDate.of(2026,7,10), LocalDate.of(2026,7,10), LeaveType.FULL_DAY);
            when(employeeRepository.findById(employee.getId())).thenReturn(Optional.of(employee));
            assertThatThrownBy(() -> service.create(employee.getId(), req))
                    .isInstanceOf(ResponseStatusException.class).hasMessageContaining("400");
        }
        @Test @DisplayName("打刻済み日→409")
        void create_alreadyClocked_409() {
            var req = new PaidLeaveCreateRequest(LocalDate.of(2026,7,14), LocalDate.of(2026,7,14), LeaveType.FULL_DAY);
            when(employeeRepository.findById(employee.getId())).thenReturn(Optional.of(employee));
            when(leaveRepository.findOverlapping(any(),any(),any(),any())).thenReturn(List.of());
            when(attendanceRecordRepository.findByEmployeeIdAndWorkDateIn(any(),any())).thenReturn(List.of(new AttendanceRecord()));
            assertThatThrownBy(() -> service.create(employee.getId(), req))
                    .isInstanceOf(ResponseStatusException.class).hasMessageContaining("409");
        }
        @Test @DisplayName("重複申請→409")
        void create_overlap_409() {
            var req = new PaidLeaveCreateRequest(LocalDate.of(2026,7,14), LocalDate.of(2026,7,14), LeaveType.FULL_DAY);
            var existing = PaidLeaveRequest.builder().id(UUID.randomUUID()).requester(employee)
                    .startDate(LocalDate.of(2026,7,14)).endDate(LocalDate.of(2026,7,14))
                    .leaveType(LeaveType.FULL_DAY).status(LeaveRequestStatus.PENDING).build();
            when(employeeRepository.findById(employee.getId())).thenReturn(Optional.of(employee));
            when(leaveRepository.findOverlapping(any(),any(),any(),any())).thenReturn(List.of(existing));
            assertThatThrownBy(() -> service.create(employee.getId(), req))
                    .isInstanceOf(ResponseStatusException.class).hasMessageContaining("409");
        }
        @Test @DisplayName("土日のみ→400")
        void create_weekendsOnly_400() {
            var req = new PaidLeaveCreateRequest(LocalDate.of(2026,7,25), LocalDate.of(2026,7,26), LeaveType.FULL_DAY);
            when(employeeRepository.findById(employee.getId())).thenReturn(Optional.of(employee));
            assertThatThrownBy(() -> service.create(employee.getId(), req))
                    .isInstanceOf(ResponseStatusException.class).hasMessageContaining("400");
        }
    }

    @Nested @DisplayName("取り下げ") class Withdraw {
        @Test @DisplayName("PENDING→WITHDRAWN")
        void withdraw_ok() {
            var leave = PaidLeaveRequest.builder().id(UUID.randomUUID()).requester(employee)
                    .startDate(LocalDate.of(2026,7,14)).endDate(LocalDate.of(2026,7,14))
                    .leaveType(LeaveType.FULL_DAY).status(LeaveRequestStatus.PENDING).version(0L).build();
            when(leaveRepository.findById(leave.getId())).thenReturn(Optional.of(leave));
            when(leaveRepository.save(any(PaidLeaveRequest.class))).thenAnswer(i -> i.getArgument(0));
            assertThat(service.withdraw(leave.getId(), employee.getId(), 0L).status()).isEqualTo(LeaveRequestStatus.WITHDRAWN);
        }
        @Test @DisplayName("APPROVED→409")
        void withdraw_approved_409() {
            var leave = PaidLeaveRequest.builder().id(UUID.randomUUID()).requester(employee)
                    .startDate(LocalDate.of(2026,7,14)).endDate(LocalDate.of(2026,7,14))
                    .leaveType(LeaveType.FULL_DAY).status(LeaveRequestStatus.APPROVED).version(0L).build();
            when(leaveRepository.findById(leave.getId())).thenReturn(Optional.of(leave));
            assertThatThrownBy(() -> service.withdraw(leave.getId(), employee.getId(), 0L))
                    .isInstanceOf(ResponseStatusException.class).hasMessageContaining("409");
        }
        @Test @DisplayName("他人→403")
        void withdraw_other_403() {
            var leave = PaidLeaveRequest.builder().id(UUID.randomUUID()).requester(employee)
                    .startDate(LocalDate.of(2026,7,14)).endDate(LocalDate.of(2026,7,14))
                    .leaveType(LeaveType.FULL_DAY).status(LeaveRequestStatus.PENDING).version(0L).build();
            when(leaveRepository.findById(leave.getId())).thenReturn(Optional.of(leave));
            assertThatThrownBy(() -> service.withdraw(leave.getId(), manager.getId(), 0L))
                    .isInstanceOf(ResponseStatusException.class).hasMessageContaining("403");
        }
    }

    @Nested @DisplayName("承認") class Approve {
        @Test @DisplayName("承認→APPROVED")
        void approve_ok() {
            var leave = PaidLeaveRequest.builder().id(UUID.randomUUID()).requester(employee)
                    .startDate(LocalDate.of(2026,7,14)).endDate(LocalDate.of(2026,7,14))
                    .leaveType(LeaveType.FULL_DAY).status(LeaveRequestStatus.PENDING).version(0L).build();
            when(leaveRepository.findById(leave.getId())).thenReturn(Optional.of(leave));
            when(employeeRepository.findById(manager.getId())).thenReturn(Optional.of(manager));
            when(leaveRepository.save(any(PaidLeaveRequest.class))).thenAnswer(i -> i.getArgument(0));
            var result = service.approve(leave.getId(), manager.getId(), 0L);
            assertThat(result.status()).isEqualTo(LeaveRequestStatus.APPROVED);
            assertThat(result.approverId()).isEqualTo(manager.getId());
        }
        @Test @DisplayName("自己承認OK")
        void approve_self() {
            var leave = PaidLeaveRequest.builder().id(UUID.randomUUID()).requester(manager)
                    .startDate(LocalDate.of(2026,7,14)).endDate(LocalDate.of(2026,7,14))
                    .leaveType(LeaveType.FULL_DAY).status(LeaveRequestStatus.PENDING).version(0L).build();
            when(leaveRepository.findById(leave.getId())).thenReturn(Optional.of(leave));
            when(employeeRepository.findById(manager.getId())).thenReturn(Optional.of(manager));
            when(leaveRepository.save(any(PaidLeaveRequest.class))).thenAnswer(i -> i.getArgument(0));
            assertThat(service.approve(leave.getId(), manager.getId(), 0L).status()).isEqualTo(LeaveRequestStatus.APPROVED);
        }
        @Test @DisplayName("他部署上長→403")
        void approve_otherDept_403() {
            var otherDept = Department.builder().id(UUID.randomUUID()).name("Sales").build();
            var otherMgr = Employee.builder().id(UUID.randomUUID()).name("鈴木").email("s@e.com")
                    .password("h").department(otherDept).role(Role.EMPLOYEE).isManager(true)
                    .hireDate(LocalDate.of(2020,4,1)).build();
            var leave = PaidLeaveRequest.builder().id(UUID.randomUUID()).requester(employee)
                    .startDate(LocalDate.of(2026,7,14)).endDate(LocalDate.of(2026,7,14))
                    .leaveType(LeaveType.FULL_DAY).status(LeaveRequestStatus.PENDING).version(0L).build();
            when(leaveRepository.findById(leave.getId())).thenReturn(Optional.of(leave));
            when(employeeRepository.findById(otherMgr.getId())).thenReturn(Optional.of(otherMgr));
            assertThatThrownBy(() -> service.approve(leave.getId(), otherMgr.getId(), 0L))
                    .isInstanceOf(ResponseStatusException.class).hasMessageContaining("403");
        }
    }

    @Nested @DisplayName("却下") class Reject {
        @Test @DisplayName("却下→REJECTED+理由保存")
        void reject_ok() {
            var leave = PaidLeaveRequest.builder().id(UUID.randomUUID()).requester(employee)
                    .startDate(LocalDate.of(2026,7,14)).endDate(LocalDate.of(2026,7,14))
                    .leaveType(LeaveType.FULL_DAY).status(LeaveRequestStatus.PENDING).version(0L).build();
            when(leaveRepository.findById(leave.getId())).thenReturn(Optional.of(leave));
            when(employeeRepository.findById(manager.getId())).thenReturn(Optional.of(manager));
            when(leaveRepository.save(any(PaidLeaveRequest.class))).thenAnswer(i -> i.getArgument(0));
            var result = service.reject(leave.getId(), manager.getId(), "繁忙期", 0L);
            assertThat(result.status()).isEqualTo(LeaveRequestStatus.REJECTED);
            assertThat(result.rejectReason()).isEqualTo("繁忙期");
        }
    }

    @Nested @DisplayName("楽観ロック") class OptimisticLock {
        @Test @DisplayName("version不一致→409")
        void versionMismatch_409() {
            var leave = PaidLeaveRequest.builder().id(UUID.randomUUID()).requester(employee)
                    .startDate(LocalDate.of(2026,7,14)).endDate(LocalDate.of(2026,7,14))
                    .leaveType(LeaveType.FULL_DAY).status(LeaveRequestStatus.PENDING).version(1L).build();
            when(leaveRepository.findById(leave.getId())).thenReturn(Optional.of(leave));
            when(employeeRepository.findById(manager.getId())).thenReturn(Optional.of(manager));
            assertThatThrownBy(() -> service.approve(leave.getId(), manager.getId(), 0L))
                    .isInstanceOf(ResponseStatusException.class).hasMessageContaining("409");
        }
    }

    @Nested @DisplayName("自分の申請一覧") class FindByRequester {
        @Test @DisplayName("ステータスなしで全件取得")
        void findByRequester_noStatus_returnsAll() {
            var leave = PaidLeaveRequest.builder().id(UUID.randomUUID()).requester(employee)
                    .startDate(LocalDate.of(2026,7,14)).endDate(LocalDate.of(2026,7,14))
                    .leaveType(LeaveType.FULL_DAY).status(LeaveRequestStatus.PENDING).build();
            when(leaveRepository.findByRequesterIdOrderByCreatedAtDesc(employee.getId()))
                    .thenReturn(List.of(leave));

            var results = service.findByRequester(employee.getId(), null);
            assertThat(results).hasSize(1);
        }

        @Test @DisplayName("ステータスフィルタで絞り込み")
        void findByRequester_withStatus_filtersCorrectly() {
            var leave = PaidLeaveRequest.builder().id(UUID.randomUUID()).requester(employee)
                    .startDate(LocalDate.of(2026,7,14)).endDate(LocalDate.of(2026,7,14))
                    .leaveType(LeaveType.FULL_DAY).status(LeaveRequestStatus.APPROVED).build();
            when(leaveRepository.findByRequesterIdAndStatusOrderByCreatedAtDesc(
                    employee.getId(), LeaveRequestStatus.APPROVED)).thenReturn(List.of(leave));

            var results = service.findByRequester(employee.getId(), LeaveRequestStatus.APPROVED);
            assertThat(results).hasSize(1);
            assertThat(results.get(0).status()).isEqualTo(LeaveRequestStatus.APPROVED);
        }
    }

    @Nested @DisplayName("承認待ち一覧") class FindPending {
        @Test @DisplayName("自部署のPENDINGのみ返す")
        void findPending_returnsDeptPending() {
            var leave = PaidLeaveRequest.builder().id(UUID.randomUUID()).requester(employee)
                    .startDate(LocalDate.of(2026,7,14)).endDate(LocalDate.of(2026,7,14))
                    .leaveType(LeaveType.FULL_DAY).status(LeaveRequestStatus.PENDING).build();
            when(employeeRepository.findById(manager.getId())).thenReturn(Optional.of(manager));
            when(leaveRepository.findByRequesterDepartmentIdAndStatusOrderByCreatedAtDesc(
                    department.getId(), LeaveRequestStatus.PENDING)).thenReturn(List.of(leave));

            var results = service.findPending(manager.getId());
            assertThat(results).hasSize(1);
        }
    }
}
