package com.example.khoaluan

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView

class CategoryDetailActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_category_detail)

        val categoryName = intent.getStringExtra("CATEGORY_NAME") ?: "Côn trùng"
        findViewById<TextView>(R.id.tvCategoryTitle).text = categoryName
        findViewById<ImageView>(R.id.btnBack).setOnClickListener { finish() }

        val rv = findViewById<RecyclerView>(R.id.rvCategoryInsects)

        val insectList = getMockDataByCategory(categoryName)

        rv.layoutManager = GridLayoutManager(this, 2)
        rv.adapter = CategoryInsectAdapter(insectList)
    }

    private fun getMockDataByCategory(name: String): List<FeaturedInsect> {
        return when (name) {
            "Phổ biến nhất" -> listOf(
                FeaturedInsect(1, "rice leaf roller", "Sâu cuốn lá lúa", R.drawable.sau_cuon_la),
                FeaturedInsect(2, "rice leaf caterpillar", "Sâu ăn lá lúa", R.drawable.sau_an_la),
                FeaturedInsect(3, "yellow rice borer", "Sâu đục thân lúa vàng", R.drawable.sau_duc_than_lua_vang),
                FeaturedInsect(4, "asiatic rice borer", "Sâu đục thân lúa Châu Á", R.drawable.sau_duc_than_lua_chau_a),
                FeaturedInsect(5, "corn borer", "Sâu đục thân ngô", R.drawable.sau_duc_than_ngo),
                FeaturedInsect(6, "peach borer", "Sâu đục thân đào", R.drawable.sau_duc_than_dao),
                FeaturedInsect(7, "black cutworm", "Sâu xám đen", R.drawable.sau_sam_den),
                FeaturedInsect(8, "large cutworm", "Sâu xám lớn", R.drawable.sau_xam_lon),
                FeaturedInsect(9, "yellow cutworm", "Sâu xám vàng", R.drawable.sau_xam_vang),
                FeaturedInsect(10, "army worm", "Sâu keo", R.drawable.sau_keo),
                FeaturedInsect(11, "cabbage army worm", "Sâu keo hại bắp cải", R.drawable.sau_keo_hai_bap_cai),
                FeaturedInsect(12, "beet army worm", "Sâu keo hại củ cải/ củ dề", R.drawable.sau_keo_hai_cu_cai),
                FeaturedInsect(13, "prodenia litura", "Sâu khoang", R.drawable.sau_khoan),
                FeaturedInsect(14, "meadow moth", "Ngài đồng cỏ", R.drawable.ngai_dong_co),
                FeaturedInsect(15, "white margined moth", "Ngài viền trắng", R.drawable.ngai_vien_trang),
                FeaturedInsect(16, "flax budworm", "Sâu đục nụ cây lanh", R.drawable.sau_duc_cai_bu_lanh),
                FeaturedInsect(17, "pieris canidia", "Bướm trắng hại rau cải", R.drawable.buom_trang_hai_rau_cai),
                FeaturedInsect(18, "papilio xuthus", "Bướm phượng chanh", R.drawable.buom_phuong_chanh),
                FeaturedInsect(19, "limacodidae", "Họ sâu róm nhớt", R.drawable.ho_sau_rom),
                FeaturedInsect(20, "ampelophaga", "Sâu đục thân nho", R.drawable.sau_duc_than_nho),
                FeaturedInsect(21, "parathrene regalis", "Sâu đục thân giả ong", R.drawable.sau_duc_than_gia_ong),
                FeaturedInsect(22, "chlumetia transversa", "Sâu đục thân xoài", R.drawable.sau_duc_than_xoai)
            )
            "Bọ cánh cứng" -> listOf(
                FeaturedInsect(23, "rice water weevil", "Bọ vòi voi hại lúa nước", R.drawable.bo_voi_voi_hai_lua_nuoc),
                FeaturedInsect(24, "flea beetle", "Bọ nhảy", R.drawable.bo_nhay),
                FeaturedInsect(25, "beet weevil", "Bọ vòi voi hại củ cải", R.drawable.bo_voi_voi_hai_cu_cai),
                FeaturedInsect(26, "alfalfa weevil", "Bọ vòi voi hại cỏ linh lăng", R.drawable.bo_voi_voi_hai_cu_linh_lang),
                FeaturedInsect(27, "legume blister beetle", "Bọ cánh cứng gây phồng da hại cây họ đậu", R.drawable.bo_canh_cung_gay_phong_da_hai_cay_ho_dau),
                FeaturedInsect(28, "blister beetle", "Bọ phòng da", R.drawable.bo_phong_da),
                FeaturedInsect(29, "lytta polita", "Bọ phồng da Lytta polita", R.drawable.bo_phong_da_lytta_polita),
                FeaturedInsect(30, "oides decempunctata", "Bọ cánh cứng mười chấm", R.drawable.bo_canh_cung_muoi_cham),
                FeaturedInsect(31, "deporaus marginatus Pascoe", "Bọ vòi voi cuốn lá", R.drawable.bo_voi_voi_cuon_la),
                FeaturedInsect(32, "salurnis marginella Guerr", "Bọ cánh cứng hại lúa", R.drawable.bo_canh_cung_hai_lua),
                FeaturedInsect(33, "sternochetus frigidus", "Bọ vòi voi hại hạt(quả)", R.drawable.bo_voi_voi_hai_qua),
                FeaturedInsect(34, "rhytidodera bowrinii White", "Bọ xén tóc Rhytidodera bowrinii", R.drawable.bo_xen_toc),
                FeaturedInsect(35, "xylotrechus", "Bọ xen tóc", R.drawable.bo_xen),
                FeaturedInsect(36, "grub", "Ấu trùng bọ cánh cứng", R.drawable.au_trung_bo_canh_cung),
                FeaturedInsect(37, "wireworm", "Sâu thép", R.drawable.sau_thep)
            )
            "Bọ xít" -> listOf(
                FeaturedInsect(38, "brown plant hopper", "Rầy nâu", R.drawable.ray_nau),
                FeaturedInsect(39, "white backed plant hopper", "Rầy lưng trắng", R.drawable.ray_lung_trang),
                FeaturedInsect(40, "small brown plant hopper", "Rầy nâu nhỏ", R.drawable.ray_nau_nho),
                FeaturedInsect(41, "rice leafhopper", "Rầy xanh hại lúa", R.drawable.ray_xanh_hai_lua),
                FeaturedInsect(42, "mango flat beak leafhopper", "Rầy đầu bẹt hại xoài", R.drawable.ray_dau_bet_hai_xoai),
                FeaturedInsect(43, "cicadella viridis", "Rầy xanh lớn", R.drawable.ray_xanh_lon),
                FeaturedInsect(44, "cicadellidae", "Họ rầy lá", R.drawable.ho_ray_la),
                FeaturedInsect(45, "alfalfa plant bug", "Bọ xích hại cỏ linh lăng", R.drawable.bo_xit_hai_linh_lang),
                FeaturedInsect(46, "tarnished plant bug", "Bọ xít mờ", R.drawable.bo_xit_mo),
                FeaturedInsect(47, "apolygus lucorum", "Bọ xít xanh", R.drawable.bo_xit_xanh),
                FeaturedInsect(48, "miridae", "Họ bọ xít mù", R.drawable.ho_bo_xit_mu),
                FeaturedInsect(49, "lycorma delicatula", "Bọ xít đèn", R.drawable.bo_xit_den),
                FeaturedInsect(50, "lawana imitata Melichar", "Rầy sáp", R.drawable.ray_sap),
                FeaturedInsect(51, "trialeurodes vaporariorum", "Bọ phấn trắng nhà kính", R.drawable.bo_phan_trang_nha_kinh),
                FeaturedInsect(52, "aleurocanthus spiniferus", "Bọ phấn đen", R.drawable.bo_phan_den)

            )
            "Ruồi" -> listOf(
                FeaturedInsect(53, "paddy stem maggot", "Ruồi đục thân lúa", R.drawable.ruoi_duc_than_lua),
                FeaturedInsect(54, "rice gall midge", "Muỗi hành lúa", R.drawable.muoi_hanh_hai_lua),
                FeaturedInsect(55, "rice stemfly", "Ruồi đục thân lúa", R.drawable.ruoi_duc_than_lua),
                FeaturedInsect(56, "wheat blossom midge", "Muỗi hành hoa lúa mì", R.drawable.muoi_hanh_hoa_lua_mi),
                FeaturedInsect(57, "cerodonta denticornis", "Ruồi đục lá", R.drawable.ruoi_duc_la),
                FeaturedInsect(58, "beet fly", "Ruồi hại củ cải", R.drawable.ruoi_hai_cu_cai),
                FeaturedInsect(59, "beet spot flies", "Ruồi đóm hại củ cải", R.drawable.ruoi_dom_hai_cu_cai),
                FeaturedInsect(60, "dasineura sp", "Muỗi hành", R.drawable.muoi_hanh),
                FeaturedInsect(61, "tetradacus c Bactrocera minax", "Ruồi đục quả cam", R.drawable.ruoi_duc_qua_cam),
                FeaturedInsect(62, "dacus dorsalis(Hendel)", "Ruoi đục quả", R.drawable.ruoi_duc_qua),
                FeaturedInsect(63, "bactrocera tsuneonis", "Ruồi đục quả quyết", R.drawable.ruoi_duc_qua_quyet)

            )
            "Châu chấu" -> listOf(
                FeaturedInsect(64, "locustoidea", "Châu chấu", R.drawable.chau_chau),
                FeaturedInsect(65, "mole cricket", "Dế dũi", R.drawable.de_dui)
            )
            "Nhện" -> listOf(
                FeaturedInsect(66, "red spider", "Nhện đỏ", R.drawable.nhen_do),
                FeaturedInsect(67, "penthaleus major", "Nhện lông lớn", R.drawable.nhen_long_lon),
                FeaturedInsect(68, "longlegged spider mite", "Nhện chân dài", R.drawable.nhen_chan_dai),
                FeaturedInsect(69, "panonchus citri McGregor", "Nhện đỏ hại cam, quyết", R.drawable.nhen_do_hai_cam_quyet),
                FeaturedInsect(70, "colomerus vitis", "Nhện nho", R.drawable.nhen_nho),
                FeaturedInsect(71, "brevipoalpus lewisi McGregor", "Nhện bẹt Lewis", R.drawable.nhen_bet),
                FeaturedInsect(72, "phyllocoptes oleiverus Ashmead", "Nhện hai ô liêu", R.drawable.nhen_hai_o_lieu)
            )
            "Bọ trĩ" -> listOf(
                FeaturedInsect(73, "grain spreader thrips", "Bọ trĩ hại ngũ cốc", R.drawable.bo_tri_hai_ngu_coc),
                FeaturedInsect(74, "wheat phloeothrips", "Bọ trĩ hại lúa mì", R.drawable.bo_tri_hai_lua_mi),
                FeaturedInsect(75, "odontothrips loti", "Bọ trĩ hại cây lạc", R.drawable.bo_tri_hai_cay_lac),
                FeaturedInsect(76, "thrips", "Bọ trĩ", R.drawable.bo_tri),
                FeaturedInsect(77, "scirtothrips dorsalis Hood", "Bọ trĩ ớt", R.drawable.bo_tri_ot)
            )
            "Ong" -> listOf(
                FeaturedInsect(78, "wheat sawfly", "Ong cưa hại lúa mì", R.drawable.ong_cua_hai_lua_mi),
                FeaturedInsect(79, "alfalfa seed chalcid", "Ong ký sinh hại linh lăng", R.drawable.ong_ky_sinh_hai_linh_lang)
            )
            else -> listOf(
                FeaturedInsect(80, "aphids", "Rệp mềm", R.drawable.rep_mem),
                FeaturedInsect(81, "english grain aphid", "Rệp lúa mì Anh", R.drawable.rep_lua_mi_anh),
                FeaturedInsect(82, "green bug", "Rệp xanh", R.drawable.rep_xanh),
                FeaturedInsect(83, "bird cherry-oataphid", "Rệp mận", R.drawable.rep_man),
                FeaturedInsect(84, "therioaphis maculata Buckton", "Rệp đốm hại cỏ linh lăng", R.drawable.rep_dom_hai_linh_lang),
                FeaturedInsect(85, "toxoptera citricidus", "Rệp nâu cam", R.drawable.rep_nau_cam),
                FeaturedInsect(86, "toxoptera aurantii", "Rệp đen cam", R.drawable.rep_den_cam),
                FeaturedInsect(87, "aphis citricola Vander Goot", "Rệp xanh cam", R.drawable.rep_xanh_cam),
                FeaturedInsect(88, "viteus vitifoliae", "Rệp nho", R.drawable.rep_nho),
                FeaturedInsect(89, "pseudococcus comstocki Kuwana", "Rệp sáp Comstock", R.drawable.rep_sap),
                FeaturedInsect(90, "icerya purchasi Maskell", "Rệp sáp bông", R.drawable.rep_sap_bong),
                FeaturedInsect(91, "unaspis yanonensis", "Rệp vẩy cứng Yanone", R.drawable.rep_vay_cung),
                FeaturedInsect(92, "ceroplastes rubens", "Rệp sáp đỏ", R.drawable.rep_sap_do),
                FeaturedInsect(93, "chrysomphalus aonidum", "Rệp vảy tròn nâu", R.drawable.rep_vay_tro_nau),
                FeaturedInsect(94, "parlatoria zizyphus lucus", "Rệp vảy Zizyphus", R.drawable.rep_vay),
                FeaturedInsect(95, "nipaecoccus vastalor", "Rệp sáp Nipaecoccus", R.drawable.rep_sap_nip)
            )
        }
    }
    inner class CategoryInsectAdapter(private val list: List<FeaturedInsect>) :
        RecyclerView.Adapter<CategoryInsectAdapter.VH>() {

        inner class VH(v: View) : RecyclerView.ViewHolder(v) {
            val img: ImageView = v.findViewById(R.id.imgInsect)
            val title: TextView = v.findViewById(R.id.tvInsectName)
            val desc: TextView = v.findViewById(R.id.tvInsectDesc)
        }

        override fun onCreateViewHolder(p: ViewGroup, t: Int) =
            VH(LayoutInflater.from(p.context).inflate(R.layout.item_featured_insect, p, false))

        override fun getItemCount() = list.size

        override fun onBindViewHolder(h: VH, p: Int) {
            val insect = list[p]

            h.title.text = insect.name
            h.desc.text = insect.description
            h.img.setImageResource(insect.imageRes)

            h.itemView.setOnClickListener {
                val intent = Intent(h.itemView.context, ResultActivity::class.java).apply {
                    putExtra("PREDICTED_NAME", insect.name)
                    putExtra("IMAGE_RES_ID", insect.imageRes)
                    putExtra("CONFIDENCE", 100f)
                }
                h.itemView.context.startActivity(intent)
            }
        }
        }
}