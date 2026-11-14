package com.yourname.got;

import com.yourname.got.support.Check;
import com.yourname.got.support.TestWorld;
import org.junit.jupiter.api.Test;

import com.yourname.got.logic.login.*;
import com.yourname.got.logic.util.Result;
import java.nio.file.*;
import java.util.List;

class LoginTest {
    static void check(boolean c, String m) { Check.that(c, m); }
    @Test
    void passwordsAreHashedAndLegacyAccountsUpgrade() throws Exception {
        TestWorld.reset(false);
        UserSystem us = new UserSystem();
        check(us.register("ali","1234").isSuccess(), "register ali/1234");
        String idx = Files.readString(Path.of("users_index.txt"));
        check(idx.contains("sha256$") && !idx.contains(",1234,"), "index stores a hash, not the password");
        check(us.login("ali","1234").isSuccess(), "login with correct password");
        check(!us.login("ali","9999").isSuccess(), "login with wrong password fails");
        check(!us.register("bad,name","1234").isSuccess(), "comma in username rejected");
        check(!us.register("bob","12ab").isSuccess(), "non-digit password rejected");
        // legacy account: plain text in the index
        String[] parts = idx.trim().split(",");
        Files.writeString(Path.of("users_index.txt"), parts[0]+",ali,1234,"+parts[3]+"\n");
        UserSystem us2 = new UserSystem();
        check(us2.login("ali","1234").isSuccess(), "legacy plain-text account can still log in");
        String after = Files.readString(Path.of("users_index.txt"));
        check(after.contains("sha256$") && !after.contains(",1234,"), "...and was upgraded to a hash");
        check(new UserSystem().login("ali","1234").isSuccess() && !new UserSystem().login("ali","0000").isSuccess(), "login still works after the upgrade");
        Check.assertAll();
    }
}
