package ru.scp.foundation.auth;

import ru.scp.foundation.dao.PersonnelDao;
import ru.scp.foundation.dao.UserDao;
import ru.scp.foundation.model.Personnel;
import ru.scp.foundation.model.User;

import java.sql.SQLException;
import java.util.Optional;

public class AuthService {

    private final UserDao userDao;
    private final PersonnelDao personnelDao;

    public AuthService() {
        this(new UserDao(), new PersonnelDao());
    }

    public AuthService(UserDao userDao, PersonnelDao personnelDao) {
        this.userDao = userDao;
        this.personnelDao = personnelDao;
    }

    /**
     * @return Session при успешном входе, иначе пустой Optional.
     */
    public Optional<Session> login(String login, String password) {
        try {
            Optional<User> userOpt = userDao.findByLogin(login);
            if (userOpt.isEmpty()) return Optional.empty();
            User user = userOpt.get();
            if (!PasswordHasher.verify(user.salt(), password, user.passwordHash())) {
                return Optional.empty();
            }
            Optional<Personnel> p = personnelDao.findById(user.personnelId());
            if (p.isEmpty()) return Optional.empty();
            return Optional.of(new Session(
                user.id(),
                user.personnelId(),
                user.login(),
                p.get().fullName(),
                user.role(),
                p.get().clearanceLevel()
            ));
        } catch (SQLException e) {
            throw new RuntimeException("Ошибка входа в систему", e);
        }
    }
}
