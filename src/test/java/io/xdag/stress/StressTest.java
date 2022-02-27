package io.xdag.stress;

import cn.hutool.json.JSONObject;
import com.google.gson.Gson;
import io.xdag.Kernel;
import io.xdag.config.Config;
import io.xdag.config.DevnetConfig;
import io.xdag.core.Block;
import io.xdag.crypto.SampleKeys;
import io.xdag.crypto.jni.Native;
import io.xdag.db.DatabaseFactory;
import io.xdag.db.DatabaseName;
import io.xdag.db.rocksdb.RocksdbFactory;
import io.xdag.db.store.BlockStore;
import io.xdag.db.store.OrphanPool;
import io.xdag.stress.common.BlockResult;
import io.xdag.stress.common.JsonCall;
import io.xdag.stress.common.ProgressBar;
import io.xdag.wallet.Wallet;
import java.math.BigInteger;
import java.security.Security;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import org.apache.tuweni.bytes.Bytes32;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.hyperledger.besu.crypto.SECP256K1;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class StressTest {
    static { Security.addProvider(new BouncyCastleProvider());  }
    private static final String url = "http://127.0.0.1:4444";

    @Rule
    public TemporaryFolder root = new TemporaryFolder();
    Config config = new DevnetConfig();
    Wallet wallet;
    String pwd;
    Kernel kernel;
    DatabaseFactory dbFactory;

    BigInteger private_1 = new BigInteger("c85ef7d79691fe79573b1a7064c19c1a9819ebdbd1faaab1a8ec92344438aaf4", 16);
    BigInteger private_2 = new BigInteger("10a55f0c18c46873ddbf9f15eddfc06f10953c601fd144474131199e04148046", 16);

    SECP256K1.PrivateKey secretkey_1 = SECP256K1.PrivateKey.create(private_1);

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

//    @Test
    public void testStress() {
        // 1. 压测区块数量
        int num = 200000;
        ProgressBar generateBar = new ProgressBar(num,30);
        ProgressBar sendBar = new ProgressBar(num,30);

        int sendSuccess = 0;
        int sendSuccessBecomeMain = 0;
        int sendSuccessExist = 0;
        int onChainAccepted = 0;
        int onChainRejected = 0;
        int onChainMain = 0;

        List<Block> blocks = new ArrayList<>();
        Map<Bytes32,Long> map = new HashMap<>();


        // 2. 创建区块
        System.out.print(new Date(System.currentTimeMillis()));
        System.out.printf(" 创建%d个区块...\n",num);
        for (int i = 0; i < num;i++) {
            SECP256K1.KeyPair addrKey = SECP256K1.KeyPair.create(secretkey_1);
            long xdagTime = System.currentTimeMillis()+i*10;
            Block b = new Block(config, xdagTime, null, null, false, null, null, -1);
            b.signOut(addrKey);
            blocks.add(b);
            generateBar.showBarByPoint("创建区块中:",i+1);
        }
        System.out.println();
        System.out.print(new Date(System.currentTimeMillis()));
        System.out.printf(" 创建完成%d个区块\n",num);


        // 3. 发起请求。
        for (int i = 0; i< num;i++) {
            JsonCall jsonCall = new JsonCall();
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("method", "xdag_sendRawTransaction");
            jsonObject.put("jsonrpc", "2.0");
            jsonObject.put("id", 1);
            List<String> list = new ArrayList<>();
            list.add(blocks.get(i).getXdagBlock().getData().toUnprefixedHexString());
            jsonObject.put("params", list);
            // 3.1 同步模型发送
//            String res = jsonCall.post(url, jsonObject.toString());
//
//            Gson gson=new Gson();
//            SendBlockResult result = gson.fromJson(res, SendBlockResult.class);
//            if(result.getResult().getStatus().equals(IMPORTED_NOT_BEST)) {
//                sendSuccess++;
//            } else if (result.getResult().getStatus().equals(IMPORTED_BEST)) {
//                sendSuccessBecomeMain++;
//            }else if (result.getResult().getStatus().equals(EXIST)) {
//                sendSuccessExist ++;
//            }
////            map.put(blocks.get(i).getHashLow(),System.currentTimeMillis());
//            sendBar.showBarByPoint("发送区块中:",i+1);
            // 3.2 异步模型发送
            jsonCall.postASync(url, jsonObject.toString());
            sendBar.showBarByPoint("发送区块中:",i+1);

        }
        System.out.println();
        System.out.print(new Date(System.currentTimeMillis()));
        System.out.printf(" 全部发送完成，%d个发送成功，%d个已经存在，%d个异常，开始检查是否上链...\n",sendSuccess+sendSuccessBecomeMain,sendSuccessExist,num-(sendSuccess+sendSuccessBecomeMain+sendSuccessExist));        // 3. 定时查询区块是否加入

        long ms = 300*1000;
        long currentTime;
        do {
            currentTime = System.currentTimeMillis();
            if (currentTime % ms == 0) {
                onChainAccepted = 0;
                onChainRejected = 0;
                onChainMain = 0;
                System.out.println("待查询数量："+blocks.size());
                ProgressBar searchBar = new ProgressBar(blocks.size(),30);
                int i = 0;
                Iterator<Block> iterator = blocks.iterator();
                while (iterator.hasNext()) {
                    Block block = iterator.next();
                    i++;
                    searchBar.showBarByPoint("查询中:",i);
                    // 发起请求
                    JsonCall jsonCall = new JsonCall();
                    JSONObject jsonObject = new JSONObject();
                    jsonObject.put("method", "xdag_getBlockStatus");
                    jsonObject.put("jsonrpc", "2.0");
                    jsonObject.put("id", 1);
                    List<String> list = new ArrayList<>();
                    list.add(block.getHashLow().toUnprefixedHexString());
                    jsonObject.put("params", list);
                    String res = jsonCall.post(url, jsonObject.toString());
                    if( res != null) {
//                    System.out.println(res);
                        Gson gson = new Gson();
                        BlockResult result = gson.fromJson(res, BlockResult.class);
                        String status = result.getResult().getStatus();
                        switch (status) {
                            case "Accepted" :{
                                onChainAccepted++;
                                iterator.remove();
                                break;
                            }
                            case "Rejected" : {
                                onChainRejected++;
                                iterator.remove();
                                break;
                            }
                            case "Main" : {
                                onChainMain++;
                                iterator.remove();
                                break;
                            }
                        }
//                        if (status.equals("Accepted") || status.equals("Rejected") || status.equals("Main")) {
//                            map.put(blocks.get(i).getHashLow(),System.currentTimeMillis()-map.get(blocks.get(i).getHashLow()));
//                        }
                    }
                }
                System.out.printf("\n剩余 %d个 pending状态\n",num - (onChainAccepted + onChainRejected + onChainMain));
            }
        } while (onChainAccepted + onChainRejected + onChainMain != num);
        System.out.print(new Date(System.currentTimeMillis()));
        System.out.printf(" %d个区块成功上链,%d个区块被拒绝,%d个区块成为主块\n",onChainAccepted,onChainRejected,onChainMain);
    }
}