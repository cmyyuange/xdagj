//package io.xdag.utils;
//
//import static io.xdag.BlockBuilder.generateAddressBlock;
//import static io.xdag.BlockBuilder.generateExtraBlock;
//import static io.xdag.BlockBuilder.generateTransactionBlock;
//import static io.xdag.core.ImportResult.IMPORTED_BEST;
//import static io.xdag.core.ImportResult.IMPORTED_NOT_BEST;
//import static io.xdag.core.XdagField.FieldType.XDAG_FIELD_IN;
//import static io.xdag.core.XdagField.FieldType.XDAG_FIELD_OUT;
//import static io.xdag.utils.BasicUtils.amount2xdag;
//import static io.xdag.utils.BasicUtils.xdag2amount;
//import static org.junit.Assert.assertArrayEquals;
//import static org.junit.Assert.assertEquals;
//import static org.junit.Assert.assertNotNull;
//import static org.junit.Assert.assertSame;
//import static org.junit.Assert.assertTrue;
//
//import com.google.common.collect.Lists;
//import io.xdag.Kernel;
//import io.xdag.config.Config;
//import io.xdag.config.DevnetConfig;
//import io.xdag.core.Address;
//import io.xdag.core.Block;
//import io.xdag.core.BlockchainImpl;
//import io.xdag.core.ImportResult;
//import io.xdag.core.XdagField;
//import io.xdag.core.XdagTopStatus;
//import io.xdag.crypto.ECKeyPair;
//import io.xdag.crypto.SampleKeys;
//import io.xdag.crypto.jni.Native;
//import io.xdag.db.DatabaseFactory;
//import io.xdag.db.DatabaseName;
//import io.xdag.db.rocksdb.RocksdbFactory;
//import io.xdag.db.store.BlockStore;
//import io.xdag.db.store.OrphanPool;
//import io.xdag.wallet.Wallet;
//import java.io.IOException;
//import java.math.BigInteger;
//import java.util.ArrayList;
//import java.util.Collections;
//import java.util.Date;
//import java.util.List;
//import org.apache.tuweni.bytes.Bytes32;
//import org.junit.After;
//import org.junit.Before;
//import org.junit.Rule;
//import org.junit.Test;
//import org.junit.rules.TemporaryFolder;
//
//public class TxHistoryUtilsTest {
//
//    @Rule
//    public TemporaryFolder root = new TemporaryFolder();
//
//    Config config = new DevnetConfig();
//    Wallet wallet;
//    String pwd;
//    Kernel kernel;
//    DatabaseFactory dbFactory;
//
//    BigInteger private_1 = new BigInteger("c85ef7d79691fe79573b1a7064c19c1a9819ebdbd1faaab1a8ec92344438aaf4", 16);
//
//
//    @Before
//    public void setUp() throws Exception {
//        config.getNodeSpec().setStoreDir(root.newFolder().getAbsolutePath());
//        config.getNodeSpec().setStoreBackupDir(root.newFolder().getAbsolutePath());
//
//        Native.init(config);
//        if (Native.dnet_crypt_init() < 0) {
//            throw new Exception("dnet crypt init failed");
//        }
//        pwd = "password";
//        wallet = new Wallet(config);
//        wallet.unlock(pwd);
//        ECKeyPair key = ECKeyPair.create(Numeric.toBigInt(SampleKeys.PRIVATE_KEY_STRING));
//        wallet.setAccounts(Collections.singletonList(key));
//        wallet.flush();
//
//        kernel = new Kernel(config);
//        dbFactory = new RocksdbFactory(config);
//
//        BlockStore blockStore = new BlockStore(
//                dbFactory.getDB(DatabaseName.INDEX),
//                dbFactory.getDB(DatabaseName.TIME),
//                dbFactory.getDB(DatabaseName.BLOCK));
//
//        blockStore.reset();
//        OrphanPool orphanPool = new OrphanPool(dbFactory.getDB(DatabaseName.ORPHANIND));
//        orphanPool.reset();
//
//        kernel.setBlockStore(blockStore);
//        kernel.setOrphanPool(orphanPool);
//        kernel.setWallet(wallet);
//    }
//
//    @After
//    public void tearDown() throws IOException {
//        wallet.delete();
//    }
//
//    @Test
//    public void testAddressBlock() {
//        ECKeyPair key = ECKeyPair.create(private_1);
//        Block addressBlock = generateAddressBlock(config, key, new Date().getTime());
//        MockBlockchain blockchain = new MockBlockchain(kernel);
//        blockchain.registerListener(new TxHistoryUtils());
//        ImportResult result = blockchain.tryToConnect(addressBlock);
//        assertSame(result, IMPORTED_BEST);
//        XdagTopStatus stats = blockchain.getXdagTopStatus();
//        assertNotNull(stats);
//        assertArrayEquals(addressBlock.getHashLow().toArray(), stats.getTop());
//        Block storedBlock = blockchain.getBlockByHash(Bytes32.wrap(stats.getTop()), false);
//        assertNotNull(storedBlock);
//        assertArrayEquals(addressBlock.getHashLow().toArray(), storedBlock.getHashLow().toArray());
//    }
//
//    @Test
//    public void testTransactionBlock() {
//        ECKeyPair addrKey = ECKeyPair.create(private_1);
//        ECKeyPair poolKey = ECKeyPair.create(Numeric.toBigInt(SampleKeys.PRIVATE_KEY_STRING));
////        Date date = fastDateFormat.parse("2020-09-20 23:45:00");
//        long generateTime = 1600616700000L;
//        // 1. add one address block
//        Block addressBlock = generateAddressBlock(config, addrKey, generateTime);
//        MockBlockchain blockchain = new MockBlockchain(kernel);
//        blockchain.registerListener(new TxHistoryUtils());
//        ImportResult result = blockchain.tryToConnect(addressBlock);
//        // import address block, result must be IMPORTED_BEST
//        assertSame(result, IMPORTED_BEST);
//        List<Address> pending = Lists.newArrayList();
//        List<Block> extraBlockList = Lists.newLinkedList();
//        Bytes32 ref = addressBlock.getHashLow();
//        // 2. create 10 mainblocks
//        for (int i = 1; i <= 10; i++) {
////            date = DateUtils.addSeconds(date, 64);
//            generateTime += 64000L;
//            pending.clear();
//            pending.add(new Address(ref, XDAG_FIELD_OUT));
//            long time = XdagTime.msToXdagtimestamp(generateTime);
//            long xdagTime = XdagTime.getEndOfEpoch(time);
//            Block extraBlock = generateExtraBlock(config, poolKey, xdagTime, pending);
//            result = blockchain.tryToConnect(extraBlock);
//            assertSame(result, IMPORTED_BEST);
//            ref = extraBlock.getHashLow();
//            extraBlockList.add(extraBlock);
//        }
//
//        // 3. make one transaction(100 XDAG) block(from No.1 mainblock to address block)
//        Address from = new Address(extraBlockList.get(0).getHashLow(), XDAG_FIELD_IN);
//        Address to = new Address(addressBlock.getHashLow(), XDAG_FIELD_OUT);
//        long xdagTime = XdagTime.getEndOfEpoch(XdagTime.msToXdagtimestamp(generateTime));
//        Block txBlock = generateTransactionBlock(config, poolKey, xdagTime - 1, from, to, xdag2amount(100.00));
//
//        // 4. local check
//        assertTrue(blockchain.canUseInput(txBlock));
//        assertTrue(blockchain.checkMineAndAdd(txBlock));
//        // 5. remote check
//        assertTrue(blockchain.canUseInput(new Block(txBlock.getXdagBlock())));
//        assertTrue(blockchain.checkMineAndAdd(txBlock));
//
//        result = blockchain.tryToConnect(txBlock);
//        // import transaction block, result may be IMPORTED_NOT_BEST or IMPORTED_BEST
//        assertTrue(result == IMPORTED_NOT_BEST || result == IMPORTED_BEST);
//        // there is 12 blocks and 10 mainblocks
//
//        pending.clear();
//        pending.add(new Address(txBlock.getHashLow()));
//        ref = extraBlockList.get(extraBlockList.size() - 1).getHashLow();
//        // 4. confirm transaction block with 3 mainblocks
//        for (int i = 1; i <= 3; i++) {
//            generateTime += 64000L;
//            pending.add(new Address(ref, XDAG_FIELD_OUT));
//            long time = XdagTime.msToXdagtimestamp(generateTime);
//            xdagTime = XdagTime.getEndOfEpoch(time);
//            Block extraBlock = generateExtraBlock(config, poolKey, xdagTime, pending);
//            blockchain.tryToConnect(extraBlock);
//            ref = extraBlock.getHashLow();
//            extraBlockList.add(extraBlock);
//            pending.clear();
//        }
//
//        Block toBlock = blockchain.getBlockStore().getBlockInfoByHash(to.getHashLow());
//        Block fromBlock = blockchain.getBlockStore().getBlockInfoByHash(from.getHashLow());
//        // block reword 1024 + 100 = 1124.0
//        assertEquals("1124.0", String.valueOf(amount2xdag(toBlock.getInfo().getAmount())));
//        // block reword 1024 - 100 = 924.0
//        assertEquals("924.0", String.valueOf(amount2xdag(fromBlock.getInfo().getAmount())));
//
//        // test two key to use
//        // 4. make one transaction(100 XDAG) block(from No.1 mainblock to address block)
//        to = new Address(extraBlockList.get(0).getHashLow(), XDAG_FIELD_IN);
//        from = new Address(addressBlock.getHashLow(), XDAG_FIELD_OUT);
//        xdagTime = XdagTime.getEndOfEpoch(XdagTime.msToXdagtimestamp(generateTime));
//
//        List<Address> refs = Lists.newArrayList();
//        refs.add(new Address(from.getHashLow(), XdagField.FieldType.XDAG_FIELD_IN, xdag2amount(50.00))); // key1
//        refs.add(new Address(to.getHashLow(), XDAG_FIELD_OUT, xdag2amount(50.00)));
//        List<ECKeyPair> keys = new ArrayList<>();
//        keys.add(addrKey);
//        Block b = new Block(config, xdagTime, refs, null, false, keys, null, -1); // orphan
//        b.signIn(addrKey);
//        b.signOut(poolKey);
//
//        txBlock = b;
//
//        // 4. local check
//        assertTrue(blockchain.canUseInput(txBlock));
//        // 5. remote check
//        assertTrue(blockchain.canUseInput(new Block(txBlock.getXdagBlock())));
//
//        result = blockchain.tryToConnect(txBlock);
//        // import transaction block, result may be IMPORTED_NOT_BEST or IMPORTED_BEST
//        assertTrue(result == IMPORTED_NOT_BEST || result == IMPORTED_BEST);
//        // there is 12 blocks and 10 mainblocks
////        assertChainStatus(12, 10, 1,1, blockchain);
//
//        pending.clear();
//        pending.add(new Address(txBlock.getHashLow()));
//        ref = extraBlockList.get(extraBlockList.size() - 1).getHashLow();
//        // 4. confirm transaction block with 3 mainblocks
//        for (int i = 1; i <= 3; i++) {
//            generateTime += 64000L;
//            pending.add(new Address(ref, XDAG_FIELD_OUT));
//            long time = XdagTime.msToXdagtimestamp(generateTime);
//            xdagTime = XdagTime.getEndOfEpoch(time);
//            Block extraBlock = generateExtraBlock(config, poolKey, xdagTime, pending);
//            blockchain.tryToConnect(extraBlock);
//            ref = extraBlock.getHashLow();
//            extraBlockList.add(extraBlock);
//            pending.clear();
//        }
//
//        toBlock = blockchain.getBlockStore().getBlockInfoByHash(to.getHashLow());
//        fromBlock = blockchain.getBlockStore().getBlockInfoByHash(from.getHashLow());
//        assertEquals("974.0", String.valueOf(amount2xdag(toBlock.getInfo().getAmount())));
//        assertEquals("1074.0", String.valueOf(amount2xdag(fromBlock.getInfo().getAmount())));
//    }
//
//    static class MockBlockchain extends BlockchainImpl {
//
//        public MockBlockchain(Kernel kernel) {
//            super(kernel);
//        }
//
//
//        @Override
//        public void startCheckMain(long period) {
////            super.startCheckMain(period);
//        }
//
//        @Override
//        public void addOurBlock(int keyIndex, Block block) {
//            return;
//        }
//
//    }
//
//}
