package io.xdag.stress;

import static org.junit.Assert.assertEquals;

import io.xdag.Kernel;
import io.xdag.config.Config;
import io.xdag.config.DevnetConfig;
import io.xdag.core.Address;
import io.xdag.core.Block;
import io.xdag.core.BlockchainImpl;
import io.xdag.core.ImportResult;
import io.xdag.core.XdagBlock;
import io.xdag.crypto.SampleKeys;
import io.xdag.crypto.jni.Native;
import io.xdag.db.DatabaseFactory;
import io.xdag.db.DatabaseName;
import io.xdag.db.rocksdb.RocksdbFactory;
import io.xdag.db.store.BlockStore;
import io.xdag.db.store.OrphanPool;
import io.xdag.wallet.Wallet;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.math.BigInteger;
import java.security.Security;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.apache.commons.lang3.time.StopWatch;
import org.apache.tuweni.bytes.Bytes32;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
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
    public void stressTest() throws IOException, ClassNotFoundException, InterruptedException {
        int num; // 总块数
        int addSize; // 每次增加的块数量
        int splitNum = 4; // 分的份数
        int searchTimes = 3; // 查询的次数
        int searchSize = 20000; // 查询的区块hash

        StopWatch stopWatch = new StopWatch();
        System.out.println(" 开始加载区块");
        // 1. 加载区块
        InputStream freader = StressTestQuery.class.getClassLoader().getResourceAsStream(fileName);
        ObjectInputStream objectInputStream = new ObjectInputStream(freader);
        List<byte[]> blocks1 = new ArrayList<>();
        blocks1 = (List<byte[]>) objectInputStream.readObject();
        objectInputStream.close();

        List<Block> blocks = new ArrayList<>();
        List<Bytes32> blockhash = new ArrayList<>();

        for (byte[] data:blocks1) {
            Block block = new Block(new XdagBlock(data));
            blocks.add(block);
        }
        num = blocks.size();

        for (int i = 0;i<searchSize;i++) {
            blockhash.add(blocks.get(i).getHashLow());
        }

        System.out.printf("加载完成%d个区块\n",num);

        MockBlockchain mockBlockchain = new MockBlockchain(kernel);

        addSize = num/splitNum;

        for (int i = 0; i< splitNum;i++) {
            System.out.printf("第%d次 增加 %d 区块\n",i+1, addSize);
            stopWatch.reset();
            stopWatch.start();
            for (int j = i*addSize; j < addSize*(i+1); j++) {
                mockBlockchain.tryToConnect(blocks.get(j));
            }
            stopWatch.stop();
            System.out.println("区块增加耗时:"+stopWatch.getTime());
            System.out.println("当前 nmain:"+mockBlockchain.getXdagStats().nmain);
            System.out.println("当前 nblocks:"+mockBlockchain.getXdagStats().nblocks);
            System.out.println("当前 nnoref:"+mockBlockchain.getXdagStats().nnoref);

            for (int z = 0; z < searchTimes ; z++) {
                stopWatch.reset();
                stopWatch.start();
                System.out.println("开始查询...");
                for (int n = 0;n<searchSize;n++) {
                    mockBlockchain.getBlockByHash(blockhash.get(n),true);
                }
                stopWatch.stop();
                System.out.println("查询耗时:"+stopWatch.getTime());
            }
        }
    }


    static class MockBlockchain extends BlockchainImpl {

        public MockBlockchain(Kernel kernel) {
            super(kernel);
        }

        @Override
        public synchronized ImportResult tryToConnect(Block block) {
            checkOrphan(); // 处理孤块
            return super.tryToConnect(block);
        }

//        @Override
//        public List<Address> getBlockFromOrphanPool(int num, long sendtime) {
//            return null;
//        }

        @Override
        public void startCheckMain(long period) {
        }

        @Override
        public void addOurBlock(int keyIndex, Block block) {
            return;
        }

    }
}
