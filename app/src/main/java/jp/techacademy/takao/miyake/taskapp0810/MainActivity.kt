package jp.techacademy.takao.miyake.taskapp0810

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import io.realm.Realm
import kotlinx.android.synthetic.main.activity_main.*
import io.realm.RealmChangeListener
import io.realm.Sort
import android.content.Intent
import android.support.v7.app.AlertDialog
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.view.View
import android.widget.Toast
import io.realm.RealmResults
import android.util.Log
import android.view.inputmethod.InputMethodManager

var category_Status: Int = 0
var category_content: String? = null
const val EXTRA_TASK = "jp.techacademy.takao.miyake.taskapp0810.TASK"

class MainActivity : AppCompatActivity(), View.OnClickListener {
    private lateinit var mRealm: Realm
    private val mRealmListener = object : RealmChangeListener<Realm> {
        override fun onChange(element: Realm) {
            reloadListView()
        }
    }

 //   var category_Status: Int = 0 //カテゴリ設定フラグ
    private lateinit var mTaskAdapter: TaskAdapter
    lateinit var taskRealmResults :RealmResults<Task>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        //カテゴリー決定ボタンの押下時処理
        this.category_button.setOnClickListener(this@MainActivity)

        //キャンセルボタン押下時の処理
        this.cancel_button.setOnClickListener(this@MainActivity)

        fab.setOnClickListener {
            val intent = Intent(this@MainActivity, InputActivity::class.java)
            startActivity(intent)
        }

        // Realmの設定
        mRealm = Realm.getDefaultInstance()
        mRealm.addChangeListener(mRealmListener)

        // ListViewの設定
        mTaskAdapter = TaskAdapter(this@MainActivity)


        // ListViewをタップしたときの処理
        listView1.setOnItemClickListener { parent, _, position, _ ->
            // 入力・編集する画面に遷移させる
            val task = parent.adapter.getItem(position) as Task
            val intent = Intent(this@MainActivity, InputActivity::class.java)
            intent.putExtra(EXTRA_TASK, task.id)
            startActivity(intent)
        }

        // ListViewを長押ししたときの処理
        listView1.setOnItemLongClickListener { parent, _, position, _ ->
            // タスクを削除する
            val task = parent.adapter.getItem(position) as Task

            // ダイアログを表示する
            val builder = AlertDialog.Builder(this@MainActivity)

            builder.setTitle("削除")
            builder.setMessage(task.title + "を削除しますか")

            builder.setPositiveButton("OK"){_, _ ->
                val results = mRealm.where(Task::class.java).equalTo("id", task.id).findAll()

                mRealm.beginTransaction()
                results.deleteAllFromRealm()
                mRealm.commitTransaction()

                val resultIntent = Intent(applicationContext, TaskAlarmReceiver::class.java)
                val resultPendingIntent = PendingIntent.getBroadcast(
                    this@MainActivity,
                    task.id,
                    resultIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT
                )
                val alarmManager = getSystemService(ALARM_SERVICE) as AlarmManager
                alarmManager.cancel(resultPendingIntent)

                reloadListView()
            }

            builder.setNegativeButton("CANCEL", null)

            val dialog = builder.create()
            dialog.show()

            true
        }

        reloadListView()
    }

    private fun reloadListView() {

        Log.d("ANDROID","category_Status１ = " + category_Status.toString())
        Log.d("ANDROID","category_content1 = " + category_content.toString())

        if (category_Status == 0) {
            // Realmデータベースから、「全てのデータを取得して新しい日時順に並べた結果」を取得
            taskRealmResults =
                mRealm.where(Task::class.java).findAll().sort("date", Sort.DESCENDING)
        }

        // Realmデータベースから、「カテゴリでフィルタリングされたデータを取得して新しい日時順に並べた結果」を取得
        else if(category_Status == 1){
            //category.text = category_content.toString()

            taskRealmResults =
                mRealm.where(Task::class.java).equalTo("category", category.text.toString())
                    .findAll().sort("date", Sort.DESCENDING)
        }
        // 上記の結果を、TaskList としてセットする
        mTaskAdapter.taskList = mRealm.copyFromRealm(taskRealmResults)

        // TaskのListView用のアダプタに渡す
        listView1.adapter = mTaskAdapter

        // 表示を更新するために、アダプターにデータが変更されたことを知らせる
        mTaskAdapter.notifyDataSetChanged()
    }

    override fun onDestroy() {
        super.onDestroy()

        mRealm.close()
    }

    override  fun onClick(v: View?) {

        if (v!!.id == R.id.category_button) {

            // "決定"ボタンをクリックした時の処理

            category_Status = 1

            val inputMethodManager: InputMethodManager
            inputMethodManager = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            inputMethodManager.hideSoftInputFromWindow(v.getWindowToken(),InputMethodManager.HIDE_NOT_ALWAYS)

            taskRealmResults =
                mRealm.where(Task::class.java).equalTo("category", category.text.toString())
                    .findAll().sort("date", Sort.DESCENDING)

            // 上記の結果を、TaskList としてセットする
            mTaskAdapter.taskList = mRealm.copyFromRealm(taskRealmResults)

            // TaskのListView用のアダプタに渡す
            listView1.adapter = mTaskAdapter

            // 表示を更新するために、アダプターにデータが変更されたことを知らせる
            mTaskAdapter.notifyDataSetChanged()
            Log.d("ANDROID","category_Status２ = " + category_Status.toString())
            category_content = category.text.toString()

            Log.d("ANDROID","category_content2 = " + category_content.toString())
        }

            // "キャンセル"ボタンをクリックした時の処理
        else if (v!!.id==R.id.cancel_button) {
            category_Status = 0
               reloadListView()
            Log.d("ANDROID","category_Status３ = " + category_Status.toString())
            category_content = ""
            Log.d("ANDROID","category_content3= " + category_content.toString())
        }

        /*else {
            Toast.makeText(this, "カテゴリー欄に何か入れてから決定ボタンをクリックしてください", Toast.LENGTH_SHORT).show()
        }*/


    }

    fun View.hideKeyboard(){
        val imm= context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(windowToken, 0)

    }

}