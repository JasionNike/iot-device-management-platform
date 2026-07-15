"""Update frontend index.html with pagination support."""
import re

with open('index.html', 'r', encoding='utf-8') as f:
    content = f.read()

changes = 0

# 1. Replace loadOta method
old_ota = "  loadOta:function(){var self=this;API.get('/ota/firmwares').then(function(r){self.otaFirmware=r.data.data||[]}).catch(function(){self.otaFirmware=[]});API.get('/ota/tasks').then(function(r){var d=r.data.data||[];self.otaTasks=d;var up=0,total=0;d.forEach(function(t){up+=(t.successCount||0);total+=(t.totalCount||0)});self.otaStats={upgraded:up,pending:total-up}}).catch(function(){self.otaTasks=[]})},"
new_ota = "  loadOta:function(){this.loadOtaFirmware();this.loadOtaTasks()},\n  loadOtaFirmware:function(){var self=this;var p={pageNum:this.otaFirmwarePage.pageNum,pageSize:this.otaFirmwarePage.pageSize};API.get('/ota/firmwares',{params:p}).then(function(r){var d=r.data.data||{};self.otaFirmware=d.list||[];self.otaFirmwarePage.total=d.total||0}).catch(function(){self.otaFirmware=[]})},\n  loadOtaTasks:function(){var self=this;var p={pageNum:this.otaTaskPage.pageNum,pageSize:this.otaTaskPage.pageSize};API.get('/ota/tasks',{params:p}).then(function(r){var d=r.data.data||{};self.otaTasks=d.list||[];self.otaTaskPage.total=d.total||0;var up=0,total=0;(d.list||[]).forEach(function(t){up+=(t.successCount||0);total+=(t.totalCount||0)});self.otaStats={upgraded:up,pending:total-up}}).catch(function(){self.otaTasks=[]})},"

if old_ota in content:
    content = content.replace(old_ota, new_ota)
    changes += 1
    print('[OK] loadOta replaced')
else:
    print('[FAIL] loadOta not found')
    idx = content.find('loadOta:function')
    if idx >= 0:
        print('  Found at', idx, ':', repr(content[idx:idx+200]))

# 2. Replace loadProducts method
old_prod = "  loadProducts:function(){var self=this;API.get('/product/list').then(function(r){var list=r.data.data||[];self.productList=list;self.products=list.map(function(p){return p.productKey});if(!self.productList.length)self.productList=[{productKey:'SENSOR-TH-001',productName:'温湿度传感器',manufacturer:'汉威科技',deviceType:'SENSOR',protocol:'MQTT',description:'工业级温湿度传感器，支持-40~125℃宽温范围'},{productKey:'ENERGY-MTR-001',productName:'智能能源采集器',manufacturer:'汉威科技',deviceType:'CONTROLLER',protocol:'MQTT',description:'三相智能采集终端，支持电压/电流/功率监测'},{productKey:'FIN-ATM-001',productName:'金融自助终端',manufacturer:'恒银金融科技',deviceType:'CONSUMER',protocol:'HTTP',description:'多功能金融自助终端，支持存取款/查询/转账'},{productKey:'CONSUMER-GW-001',productName:'消费电子网关',manufacturer:'汉威科技',deviceType:'GATEWAY',protocol:'MQTT',description:'智能家居网关，支持Zigbee/WiFi设备接入'}]})},"
new_prod = "  loadProducts:function(){var self=this;var p={pageNum:this.productPage.pageNum,pageSize:this.productPage.pageSize};API.get('/product/list',{params:p}).then(function(r){var d=r.data.data||{};var list=d.list||[];self.productList=list;self.productPage.total=d.total||0;self.products=list.map(function(p){return p.productKey})}).catch(function(){self.productList=[]})},"

if old_prod in content:
    content = content.replace(old_prod, new_prod)
    changes += 1
    print('[OK] loadProducts replaced')
else:
    print('[FAIL] loadProducts not found')
    idx = content.find('loadProducts:function')
    if idx >= 0:
        print('  Found at', idx, ':', repr(content[idx:idx+200]))

# 3. Alert pagination
old_alert = '</el-table>\n    </div>\n  </div>\n</div>\n\n<el-dialog title="处理告警"'
new_alert = '</el-table>\n      <el-pagination style="margin-top:16px;text-align:right" :current-page.sync="alertPage.pageNum" :page-size="alertPage.pageSize" :total="alertPage.total" @current-change="loadAlerts" layout="total,prev,pager,next" small></el-pagination>\n    </div>\n  </div>\n</div>\n\n<el-dialog title="处理告警"'

if old_alert in content:
    content = content.replace(old_alert, new_alert)
    changes += 1
    print('[OK] Alert pagination added')
else:
    print('[FAIL] Alert anchor not found')

# 4. Firmware pagination
old_fw = '<el-table :data="otaFirmware" border stripe size="small"><el-table-column prop="version" label="版本号" width="130"></el-table-column><el-table-column prop="productKey" label="适用产品" width="160"></el-table-column><el-table-column prop="description" label="更新说明" show-overflow-tooltip min-width="250"></el-table-column><el-table-column prop="fileSize" label="大小" width="90"><template slot-scope="s">{{(s.row.fileSize/1024).toFixed(0)}}KB</template></el-table-column><el-table-column prop="status" label="状态" width="90"><template slot-scope="s"><span :class="\'tag tag-\'+(s.row.status===\'ACTIVE\'?\'active\':\'draft\')">{{s.row.status}}</span></template></el-table-column><el-table-column label="操作" width="100"><template slot-scope="s"><el-button size="mini" @click="createOtaTask(s.row)">创建任务</el-button></template></el-table-column></el-table>'
new_fw = old_fw + '\n        <el-pagination style="margin-top:12px;text-align:right" :current-page.sync="otaFirmwarePage.pageNum" :page-size="otaFirmwarePage.pageSize" :total="otaFirmwarePage.total" @current-change="loadOtaFirmware" layout="total,prev,pager,next" small></el-pagination>'

if old_fw in content:
    content = content.replace(old_fw, new_fw)
    changes += 1
    print('[OK] Firmware pagination added')
else:
    print('[FAIL] Firmware table not found')

# 5. Task pagination
old_task = '<el-table :data="otaTasks" border stripe size="small"><el-table-column prop="taskName" label="任务名称" min-width="160"></el-table-column><el-table-column prop="productKey" label="适用产品" width="150"></el-table-column><el-table-column label="灰度进度" width="160"><template slot-scope="s"><el-progress :percentage="s.row.grayPercent||0" :stroke-width="8"></el-progress></template></el-table-column><el-table-column label="状态" width="100"><template slot-scope="s"><span :class="\'tag tag-\'+(s.row.taskStatus===\'COMPLETED\'?\'active\':(s.row.taskStatus===\'RUNNING\'?\'pending\':\'draft\'))">{{s.row.taskStatus||\'--\'}}</span></template></el-table-column><el-table-column label="操作" width="80"><template slot-scope="s"><el-button size="mini" v-if="(s.row.grayPercent||0)<100&&s.row.taskStatus!==\'COMPLETED\'" @click="advanceGray(s.row)">推进</el-button></template></el-table-column><template slot="empty"><div class="empty"><i class="el-icon-info"></i><p>暂无升级任务，请先创建固件版本后点击"创建任务"</p></div></template></el-table>'
new_task = old_task + '\n        <el-pagination style="margin-top:12px;text-align:right" :current-page.sync="otaTaskPage.pageNum" :page-size="otaTaskPage.pageSize" :total="otaTaskPage.total" @current-change="loadOtaTasks" layout="total,prev,pager,next" small></el-pagination>'

if old_task in content:
    content = content.replace(old_task, new_task)
    changes += 1
    print('[OK] Task pagination added')
else:
    print('[FAIL] Task table not found')

# 6. Product pagination
old_prod_tbl = '<el-table :data="productList" border stripe size="small"><el-table-column prop="productKey" label="产品标识" width="160"></el-table-column><el-table-column prop="productName" label="产品名称" min-width="160"></el-table-column><el-table-column prop="manufacturer" label="厂商" width="120"></el-table-column><el-table-column label="设备类型" width="100"><template slot-scope="s"><el-tag size="small" :type="s.row.deviceType===\'GATEWAY\'?\'warning\':s.row.deviceType===\'CONTROLLER\'?\'success\':s.row.deviceType===\'CONSUMER\'?\'info\':\'\'">{{s.row.deviceType===\'SENSOR\'?\'传感器\':s.row.deviceType===\'GATEWAY\'?\'网关\':s.row.deviceType===\'CONTROLLER\'?\'控制器\':\'消费电子\'}}</el-tag></template></el-table-column><el-table-column prop="protocol" label="协议" width="80"></el-table-column><el-table-column prop="description" label="描述" min-width="140" show-overflow-tooltip></el-table-column><el-table-column label="操作" width="280"><template slot-scope="s"><el-button size="mini" @click="viewProduct(s.row)">能力模型</el-button><el-button size="mini" type="warning" @click="showEditProduct(s.row)">编辑</el-button><el-button size="mini" type="danger" @click="deleteProduct(s.row.productKey)">删除</el-button></template></el-table-column></el-table>'
new_prod_tbl = old_prod_tbl + '\n      <el-pagination style="margin-top:16px;text-align:right" :current-page.sync="productPage.pageNum" :page-size="productPage.pageSize" :total="productPage.total" @current-change="loadProducts" layout="total,prev,pager,next" small></el-pagination>'

if old_prod_tbl in content:
    content = content.replace(old_prod_tbl, new_prod_tbl)
    changes += 1
    print('[OK] Product pagination added')
else:
    print('[FAIL] Product table not found')

# Write back
with open('index.html', 'w', encoding='utf-8') as f:
    f.write(content)

print(f'\nDone: {changes}/6 changes applied')
