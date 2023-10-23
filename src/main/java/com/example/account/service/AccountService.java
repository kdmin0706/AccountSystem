package com.example.account.service;

import com.example.account.domain.Account;
import com.example.account.domain.AccountUser;
import com.example.account.dto.AccountDto;
import com.example.account.exception.AccountException;
import com.example.account.repository.AccountRepository;
import com.example.account.repository.AccountUserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import javax.transaction.Transactional;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Random;
import java.util.stream.Collectors;

import static com.example.account.type.AccountStatus.IN_USE;
import static com.example.account.type.AccountStatus.UNREGISTERED;
import static com.example.account.type.ErrorCode.*;

@Service
@RequiredArgsConstructor
public class AccountService {
    private final AccountRepository accountRepository;
    private final AccountUserRepository accountUserRepository;
    /**
     *  사용자가 유/무 조회
     *  계좌에 번호를 생성하고
     *  계좌를 저장하고, 그 정보를 넘긴다.
     */
    @Transactional
    public AccountDto createAccount(Long userId, Long initialBalance) {

        AccountUser accountUser = getAccountUser(userId);
        validateCreateAccount(accountUser);

        String newAccountNumber = getNewAccountNumber();  //accountNumber 랜덤 10자리 계좌 생성

        return AccountDto.fromEntity(
                accountRepository.save(Account.builder()
                        .accountUser(accountUser)
                        .accountStatus(IN_USE)
                        .accountNumber(newAccountNumber)
                        .balance(initialBalance)
                        .registeredAt(LocalDateTime.now())
                        .build())
        );
    }

    private String getNewAccountNumber() {
        //계좌 번호 생성
        String newAccountNumber = accountRepository.findFirstByOrderByIdDesc()
                .map(account -> String.valueOf((Integer.parseInt(account.getAccountNumber())) + 1))
                .orElse(createRandomAccountNumber());

        //새로 생성한 계좌 번호가 이미 사용하고 있는 번호이면 에러 처리
        if (accountRepository.findByAccountNumber(newAccountNumber).isPresent()) {
            throw new AccountException(ACCOUNT_NUMBER_ALREADY_USE);
        }

        return newAccountNumber;
    }

    private String createRandomAccountNumber() {
        Random random = new Random();
        StringBuilder stringBuilder = new StringBuilder();

        //시중 은행에서 0부터 시작하는 계좌번호도 있기 때문에 전체 랜덤 사용
        for (int i = 0; i < 10; i++) {
            stringBuilder.append(random.nextInt(10));
        }

        return stringBuilder.toString();
    }

    private void validateCreateAccount(AccountUser accountUser) {
        //계좌번호가 10자리 정수가 넘어가면 에러 처리
        if (accountRepository.countByAccountUser(accountUser) >= 10) {
            throw new AccountException(MAX_ACCOUNT_PER_USER_10) ;
        }
    }

    @Transactional
    public Account getAccount(Long id) {
        if(id < 0){
            throw new RuntimeException("Minus");
        }
        return accountRepository.findById(id)
                .orElseThrow(() -> new AccountException(ACCOUNT_NOT_FOUND));
    }

    @Transactional
    public AccountDto deleteAccount(Long userId, String accountNumber) {
        AccountUser accountUser = getAccountUser(userId);

        Account account = accountRepository.findByAccountNumber(accountNumber)
                .orElseThrow(() -> new AccountException(ACCOUNT_NOT_FOUND));

        validateDeleteAccount(accountUser, account);

        account.setAccountStatus(UNREGISTERED);
        account.setUnRegisteredAt(LocalDateTime.now());

        accountRepository.save(account);

        return AccountDto.fromEntity(account);
    }

    private void validateDeleteAccount(AccountUser accountUser, Account account) {
        if (!Objects.equals(accountUser.getId(), account.getAccountUser().getId())) {
            throw new AccountException(USER_ACCOUNT_UN_MATCH);
        }
        if (account.getAccountStatus() == UNREGISTERED) {
            throw new AccountException(ACCOUNT_ALREADY_UNREGISTERED);
        }
        if (account.getBalance() > 0) {
            throw new AccountException(BALANCE_NOT_EMPTY);
        }
    }

    @Transactional
    public List<AccountDto> getAccountsByUserId(Long userId) {
        AccountUser accountUser = getAccountUser(userId);

        List<Account> accounts = accountRepository
                .findByAccountUser(accountUser);

        return accounts.stream()
                .map(AccountDto::fromEntity)
                .collect(Collectors.toList());
    }

    private AccountUser getAccountUser(Long userId) {
        return accountUserRepository.findById(userId)
                .orElseThrow(() -> new AccountException(USER_NOT_FOUND));
    }

}
