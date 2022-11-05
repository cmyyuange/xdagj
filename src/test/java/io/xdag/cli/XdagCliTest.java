/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2020-2030 The XdagJ Developers
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package io.xdag.cli;

import static io.xdag.wallet.WalletUtils.WALLET_PASSWORD_PROMPT;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.xdag.config.Config;
import io.xdag.config.Constants;
import io.xdag.config.DevnetConfig;
import io.xdag.config.MainnetConfig;
import io.xdag.config.TestnetConfig;
import io.xdag.crypto.Keys;
import io.xdag.crypto.Sign;
import io.xdag.utils.BytesUtils;
import io.xdag.wallet.Wallet;
import java.io.File;
import java.security.Security;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.apache.tuweni.bytes.Bytes32;
import org.hyperledger.besu.crypto.KeyPair;
import org.assertj.core.util.Lists;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.hyperledger.besu.crypto.SECPPrivateKey;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.contrib.java.lang.system.ExpectedSystemExit;
import org.junit.contrib.java.lang.system.SystemErrRule;
import org.junit.contrib.java.lang.system.SystemOutRule;
import org.mockito.Mockito;

public class XdagCliTest {

    static { Security.addProvider(new BouncyCastleProvider());  }

    @Rule
    public final ExpectedSystemExit exit = ExpectedSystemExit.none();
    @Rule
    public final SystemOutRule outRule = new SystemOutRule();
    @Rule
    public final SystemErrRule errRule = new SystemErrRule();
    private Config config;

    @Before
    public void setUp() throws Exception {
        config = new DevnetConfig();
        outRule.mute();
        errRule.mute();
    }

    @Test
    public void testMain() throws Exception {
        String[] args = {"arg1", "arg2"};
        XdagCli cli = mock(XdagCli.class);
        XdagCli.main(args, cli);
        verify(cli).start(args);
    }

    @Test
    public void testHelp() throws Exception {
        XdagCli xdagCLI = spy(new XdagCli());
        xdagCLI.start(new String[]{"--help"});
        outRule.enableLog();
        xdagCLI.printHelp();
        String helpStr = """
                usage: ./xdag.sh [options]
                    --account <action>                init|create|list
                    --changepassword                  change wallet password
                    --convertoldwallet <filename>     convert xdag old wallet.dat to private key hex
                    --dumpprivatekey <address>        print hex key
                    --enablesnapshot <snapshottime>   the parameter snapshottime uses hexadecimal
                    --help                            print help
                    --importmnemonic <mnemonic>       import HDWallet mnemonic
                    --importprivatekey <key>          import hex key
                    --loadsnapshot <filename>         load snapshot
                    --makesnapshot                    make snapshot
                    --password <password>             wallet password
                    --version                         show version
                """;
        assertEquals(helpStr, outRule.getLog());
    }

    @Test
    public void testVersion() throws Exception {
        XdagCli xdagCLI = spy(new XdagCli());
        xdagCLI.start(new String[]{"--version"});
        outRule.enableLog();
        xdagCLI.printVersion();
        assertEquals(Constants.CLIENT_VERSION + "\n", outRule.getLog());
    }

    @Test
    public void testMainnet() throws Exception {
        XdagCli xdagCLI = spy(new XdagCli());
        xdagCLI.setConfig(new MainnetConfig());

        // mock accounts
        List<KeyPair> accounts = new ArrayList<>();
        KeyPair account = Keys.createEcKeyPair();
        accounts.add(account);

        // mock wallet
        Wallet wallet = mock(Wallet.class);
        when(wallet.unlock("oldpassword")).thenReturn(true);
        when(wallet.getAccounts()).thenReturn(accounts);
        when(wallet.exists()).thenReturn(true);
        when(xdagCLI.loadWallet()).thenReturn(wallet);

        // mock passwords
        doReturn("oldpassword").when(xdagCLI).readPassword(any());
        doReturn(null).when(xdagCLI).startKernel(any(), any());

        xdagCLI.start(new String[]{""});
        assertTrue(xdagCLI.getConfig() instanceof MainnetConfig);
    }

    @Test
    public void testTestnet() throws Exception {
        XdagCli xdagCLI = spy(new XdagCli());
        xdagCLI.setConfig(new TestnetConfig());

        // mock accounts
        List<KeyPair> accounts = new ArrayList<>();
        KeyPair account = Keys.createEcKeyPair();
        accounts.add(account);

        // mock wallet
        Wallet wallet = mock(Wallet.class);
        when(wallet.unlock("oldpassword")).thenReturn(true);
        when(wallet.getAccounts()).thenReturn(accounts);
        when(wallet.exists()).thenReturn(true);
        when(xdagCLI.loadWallet()).thenReturn(wallet);

        // mock passwords
        doReturn("oldpassword").when(xdagCLI).readPassword(WALLET_PASSWORD_PROMPT);
        doReturn(null).when(xdagCLI).startKernel(any(), any());

        xdagCLI.start(new String[]{"-t"});
        assertTrue(xdagCLI.getConfig() instanceof TestnetConfig);
    }

    @Test
    public void testDevnet() throws Exception {
        XdagCli xdagCLI = spy(new XdagCli());
        xdagCLI.setConfig(new DevnetConfig());

        // mock accounts
        List<KeyPair> accounts = new ArrayList<>();
        KeyPair account = Keys.createEcKeyPair();
        accounts.add(account);

        // mock wallet
        Wallet wallet = mock(Wallet.class);
        when(wallet.unlock("oldpassword")).thenReturn(true);
        when(wallet.getAccounts()).thenReturn(accounts);
        when(wallet.exists()).thenReturn(true);
        when(xdagCLI.loadWallet()).thenReturn(wallet);

        // mock passwords
        doReturn("oldpassword").when(xdagCLI).readPassword(WALLET_PASSWORD_PROMPT);
        doReturn(null).when(xdagCLI).startKernel(any(), any());

        xdagCLI.start(new String[]{"-d"});
        assertTrue(xdagCLI.getConfig() instanceof DevnetConfig);
    }

    @Test
    public void testLoadAndUnlockWalletWithWrongPassword() {
        XdagCli xdagCLI = spy(new XdagCli());
        xdagCLI.setConfig(config);
        // mock wallet
        Wallet wallet = mock(Wallet.class);
        when(wallet.unlock(any())).thenReturn(false);
        when(xdagCLI.loadWallet()).thenReturn(wallet);

        // mock password
        when(xdagCLI.getPassword()).thenReturn("password");

    }

    @Test
    public void testStartKernelWithEmptyWallet() throws Exception {
        XdagCli xdagCLI = spy(new XdagCli());
        xdagCLI.setConfig(config);
        // mock wallet
        Wallet wallet = mock(Wallet.class);
        when(wallet.exists()).thenReturn(false);
        when(wallet.unlock("oldpassword")).thenReturn(true);
        doReturn(new ArrayList<KeyPair>(), // returns empty wallet
                Collections.singletonList(Keys.createEcKeyPair()) // returns wallet with a newly created account
        ).when(wallet).getAccounts();
        when(wallet.addAccount(any(KeyPair.class))).thenReturn(true);
        when(wallet.flush()).thenReturn(true);
        when(wallet.isHdWalletInitialized()).thenReturn(true);

        // mock CLI
        when(xdagCLI.loadWallet()).thenReturn(wallet);
        doReturn(null).when(xdagCLI).startKernel(any(), any());

        // mock new account
        KeyPair newAccount = Keys.createEcKeyPair();
        when(wallet.addAccountRandom()).thenReturn(newAccount);
        when(wallet.addAccountWithNextHdKey()).thenReturn(newAccount);

        // mock passwords
        doReturn("oldpassword").when(xdagCLI).readPassword(WALLET_PASSWORD_PROMPT);
        doReturn("oldpassword").when(xdagCLI).readNewPassword(any(), any());
        doReturn(null).when(xdagCLI).startKernel(any(), any());

        // execution
        xdagCLI.start();

        // verifies that a new account is added the empty wallet
        verify(wallet).unlock("oldpassword");
        verify(wallet, times(1)).getAccounts();
        verify(wallet).addAccountWithNextHdKey();

        verify(wallet, atLeastOnce()).flush();

        // verifies that kernel starts
        verify(xdagCLI).startKernel(any(), any());
    }

    @Test
    public void testStartKernelWithEmptyWalletInvalidNewPassword() throws Exception {
        XdagCli xdagCLI = spy(new XdagCli());
        xdagCLI.setConfig(config);
        // mock wallet
        Wallet wallet = mock(Wallet.class);
        when(wallet.exists()).thenReturn(false);

        // mock CLI
        when(xdagCLI.loadWallet()).thenReturn(wallet);
        doReturn(null).when(xdagCLI).startKernel(any(), any());

        // mock password
        doReturn("a").doReturn("b").when(xdagCLI).readPassword(any());

        // mock exits
        doNothing().when(xdagCLI).exit(anyInt());

        exit.expectSystemExitWithStatus(-1);
        // execution
        xdagCLI.start();
    }

    @Test
    public void testAccountInit() throws Exception {
        XdagCli xdagCLI = spy(new XdagCli());
        xdagCLI.setConfig(config);
        Mockito.doNothing().when(xdagCLI).initHDAccount();
        xdagCLI.start(new String[]{"--account", "init"});
        verify(xdagCLI).initHDAccount();
    }

    @Test
    public void testAccountCreate() throws Exception {
        XdagCli xdagCLI = spy(new XdagCli());
        xdagCLI.setConfig(config);
        Mockito.doNothing().when(xdagCLI).createAccount();
        xdagCLI.start(new String[]{"--account", "create"});
        verify(xdagCLI).createAccount();
    }

    @Test
    public void testAccountList() throws Exception {
        XdagCli xdagCLI = spy(new XdagCli());
        xdagCLI.setConfig(config);
        Mockito.doNothing().when(xdagCLI).listAccounts();
        xdagCLI.start(new String[]{"--account", "list"});
        verify(xdagCLI).listAccounts();
    }

    @Test
    public void testCreateAccount() throws Exception {
        XdagCli xdagCLI = spy(new XdagCli());
        xdagCLI.setConfig(config);
        // mock wallet
        Wallet wallet = mock(Wallet.class);
        when(wallet.unlock("oldpassword")).thenReturn(true);
        when(wallet.isHdWalletInitialized()).thenReturn(true);
        when(wallet.addAccount(any(KeyPair.class))).thenReturn(true);
        when(wallet.flush()).thenReturn(true);
        when(xdagCLI.loadWallet()).thenReturn(wallet);

        // mock account
        KeyPair newAccount = Keys.createEcKeyPair();
        when(wallet.addAccountRandom()).thenReturn(newAccount);
        when(wallet.addAccountWithNextHdKey()).thenReturn(newAccount);

        // mock passwords
        doReturn("oldpassword").when(xdagCLI).readPassword(WALLET_PASSWORD_PROMPT);
        doReturn(null).when(xdagCLI).startKernel(any(), any());

        // execution
        xdagCLI.createAccount();

        // verification
        verify(wallet).addAccountWithNextHdKey();

        verify(wallet).flush();
    }

    @Test
    public void testListAccounts() throws Exception {
        XdagCli xdagCLI = spy(new XdagCli());
        xdagCLI.setConfig(config);
        // mock accounts
        List<KeyPair> accounts = new ArrayList<>();
        KeyPair account = Keys.createEcKeyPair();
        accounts.add(account);

        // mock wallet
        Wallet wallet = mock(Wallet.class);
        when(wallet.unlock("oldpassword")).thenReturn(true);
        when(wallet.getAccounts()).thenReturn(accounts);
        when(xdagCLI.loadWallet()).thenReturn(wallet);

        // mock passwords
        doReturn("oldpassword").when(xdagCLI).readPassword(WALLET_PASSWORD_PROMPT);
        doReturn(null).when(xdagCLI).startKernel(any(), any());

        // execution
        xdagCLI.listAccounts();

        // verification
        verify(wallet).getAccounts();
    }

    @Test
    public void testChangePasswordIncorrectConfirmation() {
        XdagCli xdagCLI = spy(new XdagCli());
        xdagCLI.setConfig(config);

        // mock wallet
        Wallet wallet = mock(Wallet.class);
        when(wallet.unlock("oldpassword")).thenReturn(true);
        when(wallet.flush()).thenReturn(true);
        when(xdagCLI.loadWallet()).thenReturn(wallet);
        when(wallet.isHdWalletInitialized()).thenReturn(true);

        // mock passwords
        doReturn("oldpassword").when(xdagCLI).readPassword(WALLET_PASSWORD_PROMPT);
        doReturn("a").doReturn("b").when(xdagCLI).readPassword(any());
        doNothing().when(xdagCLI).exit(anyInt());

        // execution
        xdagCLI.changePassword();
    }

    @Test
    public void testDumpPrivateKey() throws Exception {
        XdagCli xdagCLI = spy(new XdagCli());
        xdagCLI.setConfig(config);

        // mock account
        KeyPair account = spy(Keys.createEcKeyPair());
        String address = BytesUtils.toHexString(Keys.toBytesAddress(account));
        byte[] addressBytes = Keys.toBytesAddress(account);

        // mock wallet
        Wallet wallet = mock(Wallet.class);
        when(wallet.unlock("oldpassword")).thenReturn(true);
        when(xdagCLI.loadWallet()).thenReturn(wallet);
        when(wallet.getAccount(addressBytes)).thenReturn(account);
        when(wallet.isHdWalletInitialized()).thenReturn(true);

        // mock passwords
        doReturn("oldpassword").when(xdagCLI).readPassword(WALLET_PASSWORD_PROMPT);
        doReturn(null).when(xdagCLI).startKernel(any(), any());

        // execution
        xdagCLI.dumpPrivateKey(address);

        // verification
        verify(wallet).getAccount(addressBytes);
        verify(account).getPrivateKey();
    }

    @Test
    public void testDumpPrivateKeyNotFound() throws Exception {
        XdagCli xdagCLI = spy(new XdagCli());
        xdagCLI.setConfig(config);
        // mock address
        String address = "c583b6ad1d1cccfc00ae9113db6408f022822b20";

        byte[] addressBytes = BytesUtils.hexStringToBytes(address);

        // mock wallet
        Wallet wallet = mock(Wallet.class);
        when(wallet.unlock("oldpassword")).thenReturn(true);
        when(xdagCLI.loadWallet()).thenReturn(wallet);
        when(wallet.getAccount(addressBytes)).thenReturn(null);
        when(wallet.isHdWalletInitialized()).thenReturn(true);

        // mock passwords
        doReturn("oldpassword").when(xdagCLI).readPassword(WALLET_PASSWORD_PROMPT);
        doReturn(null).when(xdagCLI).startKernel(any(), any());

        // execution
        xdagCLI.dumpPrivateKey(address);
    }

    @Test
    public void testImportPrivateKeyExisted() throws Exception {
        XdagCli xdagCLI = spy(new XdagCli());
        xdagCLI.setConfig(config);

        // mock private key
        KeyPair keypair = Keys.createEcKeyPair();
        String key = BytesUtils.toHexString(Keys.toBytesAddress(keypair));

        // mock wallet
        Wallet wallet = mock(Wallet.class);
        when(wallet.unlock("oldpassword")).thenReturn(true);
        when(xdagCLI.loadWallet()).thenReturn(wallet);
        when(wallet.addAccount(any(KeyPair.class))).thenReturn(false);
        when(wallet.isHdWalletInitialized()).thenReturn(true);
        when(wallet.exists()).thenReturn(true);
        // mock passwords
        doReturn("oldpassword").when(xdagCLI).readPassword(WALLET_PASSWORD_PROMPT);
        doReturn(null).when(xdagCLI).startKernel(any(), any());

        // execution
        xdagCLI.importPrivateKey(key);
    }

    @Test
    public void testImportPrivateKeyFailedToFlushWalletFile() throws Exception {
        XdagCli xdagCLI = spy(new XdagCli());
        xdagCLI.setConfig(config);

        // mock private key
        KeyPair keypair = Keys.createEcKeyPair();
        String key = BytesUtils.toHexString(Keys.toBytesAddress(keypair));

        // mock wallet
        Wallet wallet = mock(Wallet.class);
        when(wallet.unlock("oldpassword")).thenReturn(true);
        when(xdagCLI.loadWallet()).thenReturn(wallet);
        when(wallet.addAccount(any(KeyPair.class))).thenReturn(true);
        when(wallet.flush()).thenReturn(false);
        when(wallet.isHdWalletInitialized()).thenReturn(true);
        when(wallet.exists()).thenReturn(true);
        // mock passwords
        doReturn("oldpassword").when(xdagCLI).readPassword(WALLET_PASSWORD_PROMPT);
        doReturn(null).when(xdagCLI).startKernel(any(), any());

        // execution
        xdagCLI.importPrivateKey(key);
    }

    @Test
    public void testImportPrivateKey() throws Exception {
        XdagCli xdagCLI = spy(new XdagCli());
        xdagCLI.setConfig(config);

        // mock private key
        final String key = "99136d0be3f8cf02f024e86542491d276d518b9813997b8e4585e652d119fbca";

        // mock wallet
        Wallet wallet = mock(Wallet.class);
        when(wallet.unlock("oldpassword")).thenReturn(true);
        when(xdagCLI.loadWallet()).thenReturn(wallet);
        when(wallet.addAccount(any(KeyPair.class))).thenReturn(true);
        when(wallet.flush()).thenReturn(true);
        when(wallet.isHdWalletInitialized()).thenReturn(true);

        when(wallet.exists()).thenReturn(false);
        //when(xdagCLI.readPassword())
        // mock passwords
        doReturn("oldpassword").when(xdagCLI).readNewPassword("EnterNewPassword:", "ReEnterNewPassword:");
        doReturn("oldpassword").when(xdagCLI).readPassword(WALLET_PASSWORD_PROMPT);
        doReturn(null).when(xdagCLI).startKernel(any(), any());

        // execution
        xdagCLI.importPrivateKey(key);
    }

    @Test
    public void testImportMnemonic() throws Exception {
        XdagCli xdagCLI = spy(new XdagCli());
        xdagCLI.setConfig(config);

        // mock private key
        final String errorMnemonic = "aaa bbb ccc";
        final String rightMnemonic = "view cycle bag maple hill famous black doll episode fine congress april";

        // mock wallet
        Wallet wallet = mock(Wallet.class);
        when(wallet.unlock("oldpassword")).thenReturn(true);
        when(xdagCLI.loadWallet()).thenReturn(wallet);
        when(wallet.flush()).thenReturn(true);

        // mock passwords
        doReturn("oldpassword").when(xdagCLI).readPassword(WALLET_PASSWORD_PROMPT);
        doReturn(null).when(xdagCLI).startKernel(any(), any());

        // execution
        assertFalse(xdagCLI.importMnemonic(errorMnemonic));
        assertTrue(xdagCLI.importMnemonic(rightMnemonic));
    }

}