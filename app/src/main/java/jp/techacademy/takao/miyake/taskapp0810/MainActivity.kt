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
import android.view.View
import android.widget.Toast
import io.realm.RealmResults
import android.util.Log


const val EXTRA_TASK = "jp.techacademy.takao.miyake.taskapp0810.TASK"

class MainActivity : AppCompatActivity(), View.OnClickListener {
    private lateinit var mRealm: Realm
    private val mRealmListener = object : RealmChangeListener<Realm> {
        override fun onChange(element: Realm) {
            reloadListView()
        }
    }

    private lateinit var mTaskAdapter: TaskAdapter
    lateinit var taskRealmResults :RealmResults<Task>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        fab.setOnClickListener {
            val intent = Intent(this@MainActivity, InputActivity::class.java)
            startActivity(intent)
        }

        // Realmの設定
        mRealm = Realm.getDefaultInstance()
        mRealm.addChangeListener(mRealmListener)

        // ListViewの設定
        mTaskAdapter = TaskAdapter(this@MainActivity)

        //カテゴリー決定ボタンの押下時処理
        category_button.setOnClickListener(this@MainActivity)

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
        // Realmデータベースから、「全てのデータを取得して新しい日時順に並べた結果」を取得
        taskRealmResults = mRealm.where(Task::class.java).findAll().sort("date",Sort.DESCENDING)

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


            // ボタンが"決定"になっている時の処理

            Log.d("ANDROID", "onClick内の決定ボタン判定前")

            if (category_button.text.toString() == "決定") {
                category_button.text = "キャンセル"

                taskRealmResults =
                    mRealm.where(Task::class.java).equalTo("category", category.text.toString())
                        .findAll().sort("date", Sort.DESCENDING)

                // 上記の結果を、TaskList としてセットする
                mTaskAdapter.taskList = mRealm.copyFromRealm(taskRealmResults)

                // TaskのListView用のアダプタに渡す
                listView1.adapter = mTaskAdapter

                // 表示を更新するために、アダプターにデータが変更されたことを知らせる
                mTaskAdapter.notifyDataSetChanged()

            }

            // ボタンが"キャンセル"になっている時の処理
            if (category_button.text.toString() == "キャンセル") {

                this.onDestroy()
                category_button.text = "決定"

            }
        } else {
            Toast.makeText(this, "カテゴリー欄に何か入れてから決定ボタンをクリックしてください", Toast.LENGTH_SHORT).show()
        }

        
    }
}