package io.xdag.stress;

import static io.xdag.BlockBuilder.generateAddressBlock;
import static io.xdag.core.ImportResult.IMPORTED_BEST;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;

import io.xdag.Kernel;
import io.xdag.config.Config;
import io.xdag.config.DevnetConfig;
import io.xdag.core.Block;
import io.xdag.core.BlockchainImpl;
import io.xdag.core.BlockchainTest;
import io.xdag.core.ImportResult;
import io.xdag.core.XdagBlock;
import io.xdag.core.XdagTopStatus;
import io.xdag.crypto.SampleKeys;
import io.xdag.crypto.jni.Native;
import io.xdag.db.DatabaseFactory;
import io.xdag.db.DatabaseName;
import io.xdag.db.rocksdb.RocksdbFactory;
import io.xdag.db.store.BlockStore;
import io.xdag.db.store.OrphanPool;
import io.xdag.stress.common.BlockResult;
import io.xdag.stress.common.JsonCall;
import io.xdag.wallet.Wallet;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.math.BigInteger;
import java.security.Security;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import org.apache.commons.lang3.time.StopWatch;
import org.apache.tuweni.bytes.Bytes32;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.hyperledger.besu.crypto.SECP256K1;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class StressTestQuery {

    static { Security.addProvider(new BouncyCastleProvider());  }

    @Rule
    public TemporaryFolder root = new TemporaryFolder();

    Config config = new DevnetConfig();
    Wallet wallet;
    String pwd;
    Kernel kernel;
    DatabaseFactory dbFactory;

    private static final String fileName = "block.data";

    BigInteger private_1 = new BigInteger("c85ef7d79691fe79573b1a7064c19c1a9819ebdbd1faaab1a8ec92344438aaf4", 16);
    BigInteger private_2 = new BigInteger("10a55f0c18c46873ddbf9f15eddfc06f10953c601fd144474131199e04148046", 16);

    org.hyperledger.besu.crypto.SECP256K1.PrivateKey secretkey_1 = org.hyperledger.besu.crypto.SECP256K1.PrivateKey.create(private_1);
    org.hyperledger.besu.crypto.SECP256K1.PrivateKey secretkey_2 = org.hyperledger.besu.crypto.SECP256K1.PrivateKey.create(private_2);

    private static void assertChainStatus(long nblocks, long nmain, long nextra, long norphan, BlockchainImpl bci) {
        assertEquals("blocks:", nblocks, bci.getXdagStats().nblocks);
        assertEquals("main:", nmain, bci.getXdagStats().nmain);
        assertEquals("nextra:", nextra, bci.getXdagStats().nextra);
        assertEquals("orphan:", norphan, bci.getXdagStats().nnoref);
    }

    @Before
    public void setUp() throws Exception {
        config.getNodeSpec().setStoreDir(root.newFolder().getAbsolutePath());
        config.getNodeSpec().setStoreBackupDir(root.newFolder().getAbsolutePath());

        Native.init(config);
        if (Native.dnet_crypt_init() < 0) {
            throw new Exception("dnet crypt init failed");
        }
        pwd = "password";
        wallet = new Wallet(config);
        wallet.unlock(pwd);
        org.hyperledger.besu.crypto.SECP256K1.KeyPair key = org.hyperledger.besu.crypto.SECP256K1.KeyPair.create(
                SampleKeys.SRIVATE_KEY);
        wallet.setAccounts(Collections.singletonList(key));
        wallet.flush();

        kernel = new Kernel(config);
        dbFactory = new RocksdbFactory(config);

        BlockStore blockStore = new BlockStore(
                dbFactory.getDB(DatabaseName.INDEX),
                dbFactory.getDB(DatabaseName.TIME),
                dbFactory.getDB(DatabaseName.BLOCK));

        blockStore.reset();
        OrphanPool orphanPool = new OrphanPool(dbFactory.getDB(DatabaseName.ORPHANIND));
        orphanPool.reset();

        kernel.setBlockStore(blockStore);
        kernel.setOrphanPool(orphanPool);
        kernel.setWallet(wallet);
    }

    @After
    public void tearDown() throws IOException {
        wallet.delete();
    }


    @Test
    public void stressTest() throws IOException, ClassNotFoundException {

        StopWatch stopWatch = new StopWatch();

        System.out.println(" 开始加载区块");
        stopWatch.start();

        InputStream freader = StressTestByLoadBlocks.class.getClassLoader().getResourceAsStream(fileName);
        ObjectInputStream objectInputStream = new ObjectInputStream(freader);
        List<byte[]> blocks1 = new ArrayList<>();
        blocks1 = (List<byte[]>) objectInputStream.readObject();
        objectInputStream.close();

        List<Block> blocks = new ArrayList<>();


        List<Bytes32> blockhash1 = new ArrayList<>();
        List<Bytes32> blockhash2 = new ArrayList<>();

        for (byte[] data:blocks1) {
            Block block = new Block(new XdagBlock(data));
            blocks.add(block);
        }
        int num = blocks1.size();

        for (int i = 0;i<num;i++) {
            if (i<num/2) {
                blockhash1.add(blocks.get(i).getHashLow());
            } else {
                blockhash2.add(blocks.get(i).getHashLow());
            }
        }

        stopWatch.stop();

        // 1. 加载区块
        System.out.printf(" 加载完成%d个区块\n",num);
        System.out.printf(" blocks hash1 加载完成%d个区块\n",blockhash1.size());
        System.out.printf(" blocks hash2 加载完成%d个区块\n",blockhash2.size());


        MockBlockchain mockBlockchain = new MockBlockchain(kernel);

        for (int i = 0; i<num/2;i++) {
            mockBlockchain.tryToConnect(blocks.get(i));
        }

        stopWatch.reset();
        stopWatch.start();
        for (Bytes32 hashlow : blockhash1) {
            Block block = mockBlockchain.getBlockByHash(hashlow,true);
        }
        stopWatch.stop();
        System.out.println("增加到100000个区块后，查询前100000个区块");
        System.out.println("耗时"+stopWatch.getTime());

        for (int i = num/2;i<num; i++) {
            mockBlockchain.tryToConnect(blocks.get(i));
        }

        stopWatch.reset();
        stopWatch.start();
        for (Bytes32 hashlow : blockhash1) {
            Block block = mockBlockchain.getBlockByHash(hashlow,true);
        }
        stopWatch.stop();
        System.out.println("增加到200000个区块后，查询前100000个区块");
        System.out.println("耗时"+stopWatch.getTime());

        stopWatch.reset();
        stopWatch.start();
        for (Bytes32 hashlow : blockhash2) {
            Block block = mockBlockchain.getBlockByHash(hashlow,true);
        }
        stopWatch.stop();
        System.out.println("增加到200000个区块后，查询后100000个区块");
        System.out.println("耗时"+stopWatch.getTime());
    }


    @Test
    public void testAddressBlock() {
        org.hyperledger.besu.crypto.SECP256K1.KeyPair key = SECP256K1.KeyPair.create(secretkey_1);
        Block addressBlock = generateAddressBlock(config, key, new Date().getTime());
        MockBlockchain blockchain = new MockBlockchain(kernel);
        ImportResult result = blockchain.tryToConnect(addressBlock);
        assertSame(result, IMPORTED_BEST);
        XdagTopStatus stats = blockchain.getXdagTopStatus();
        assertNotNull(stats);
        assertArrayEquals(addressBlock.getHashLow().toArray(), stats.getTop());
        Block storedBlock = blockchain.getBlockByHash(Bytes32.wrap(stats.getTop()), false);
        assertNotNull(storedBlock);
        assertArrayEquals(addressBlock.getHashLow().toArray(), storedBlock.getHashLow().toArray());
    }

    static class MockBlockchain extends BlockchainImpl {

        public MockBlockchain(Kernel kernel) {
            super(kernel);
        }


        @Override
        public void startCheckMain(long period) {
//            super.startCheckMain(period);
        }

        @Override
        public void addOurBlock(int keyIndex, Block block) {
            return;
        }

    }
}
